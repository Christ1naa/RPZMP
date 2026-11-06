import asyncio
import json
import logging
import os
import secrets
import time
from copy import deepcopy
from pathlib import Path

import jwt
from fastapi import Depends, FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, Response
from pydantic import BaseModel, ConfigDict
from starlette.exceptions import HTTPException as StarletteHTTPException


logger = logging.getLogger("uvicorn.error")

FIXTURE_PATH = Path(__file__).parent / "fixtures" / "scenarios.json"

# Вигадані акаунти лише для навчального локального сервера.
ACCOUNTS = {
    "demo@energy.test": {
        "id": "demo-user",
        "name": "Демо користувач",
        "password": "Demo1234",
        "role": "USER",
    },
    "admin@energy.test": {
        "id": "demo-admin",
        "name": "Адміністратор",
        "password": "Admin1234",
        "role": "ADMIN",
    },
}

FAULT_MODES = {
    "none",
    "transient503",
    "always503",
    "malformed",
    "invalid",
    "timeout",
}


class LoginRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    email: str
    password: str


class ApiError(Exception):
    """Очікувана помилка API зі стабільним кодом для Android."""

    def __init__(self, status: int, code: str, message: str):
        self.status = status
        self.code = code
        self.message = message


def require_user(request: Request) -> dict:
    """Перевіряє підпис, строк дії та власника токена на сервері."""
    authorization = request.headers.get("Authorization")

    if not authorization:
        raise ApiError(401, "TOKEN_REQUIRED", "Потрібно увійти")

    scheme, separator, token = authorization.partition(" ")

    if scheme.lower() != "bearer" or not separator or not token.strip():
        raise ApiError(401, "INVALID_TOKEN", "Некоректний токен")

    try:
        payload = jwt.decode(
            token.strip(),
            request.app.state.jwt_secret,
            algorithms=["HS256"],
            options={
                "require": ["sub", "email", "name", "role", "iat", "exp"]
            },
        )
    except jwt.ExpiredSignatureError:
        raise ApiError(
            401,
            "TOKEN_EXPIRED",
            "Сесію завершено. Увійдіть повторно",
        ) from None
    except jwt.InvalidTokenError:
        raise ApiError(401, "INVALID_TOKEN", "Некоректний токен") from None

    email = payload.get("email")
    account = ACCOUNTS.get(email) if isinstance(email, str) else None

    if (
        account is None
        or payload.get("sub") != account["id"]
        or payload.get("role") != account["role"]
    ):
        raise ApiError(401, "INVALID_TOKEN", "Некоректний токен")

    return payload


def require_admin(user: dict = Depends(require_user)) -> dict:
    """Прихована кнопка в Android не замінює перевірку прав API."""
    if user["role"] != "ADMIN":
        raise ApiError(403, "FORBIDDEN", "Потрібні права адміністратора")

    return user


async def apply_fault(request: Request):
    """Керовані збої застосовуються після перевірки авторизації."""
    state = request.app.state
    path = request.url.path
    attempt = state.request_counts.get(path, 0) + 1
    state.request_counts[path] = attempt

    # Логуємо лише шлях і номер спроби, без токенів та паролів.
    logger.info(
        "scenario_request path=%s attempt=%s mode=%s",
        path,
        attempt,
        state.fault,
    )

    if state.fault == "always503" or (
        state.fault == "transient503" and attempt <= 2
    ):
        raise ApiError(
            503,
            "TEMPORARILY_UNAVAILABLE",
            "Сервіс тимчасово недоступний",
        )

    if state.fault == "timeout":
        await asyncio.sleep(30)

    if state.fault == "malformed":
        return Response(
            content='{"scenarios":',
            media_type="application/json",
        )

    if state.fault == "invalid":
        # Навмисно порушуємо контракт для перевірки Android-валідатора.
        data = deepcopy(state.catalog)
        data["scenarios"][0]["devices"][0]["powerWatts"] = -60.0

        if path == "/scenarios":
            return JSONResponse(content=data)

        return JSONResponse(content=data["scenarios"][0])

    return None


def create_app(
    ttl_seconds: int = 3600,
    fault: str = "none",
    secret: str | None = None,
) -> FastAPI:
    """Окремий екземпляр спрощує ізольовані серверні тести."""
    if ttl_seconds <= 0:
        raise ValueError("Строк дії токена має бути додатним")

    if fault not in FAULT_MODES:
        raise ValueError(f"Невідомий режим збою: {fault}")

    app = FastAPI(title="EnergyForecast Training API", version="1.0")

    app.state.jwt_secret = secret or secrets.token_urlsafe(32)
    app.state.ttl_seconds = ttl_seconds
    app.state.fault = fault
    app.state.request_counts = {}
    app.state.catalog = json.loads(
        FIXTURE_PATH.read_text(encoding="utf-8")
    )

    @app.exception_handler(ApiError)
    async def api_error_handler(request: Request, error: ApiError):
        headers = (
            {"WWW-Authenticate": "Bearer"}
            if error.status == 401
            else None
        )

        return JSONResponse(
            status_code=error.status,
            content={
                "error": {
                    "code": error.code,
                    "message": error.message,
                }
            },
            headers=headers,
        )

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(
        request: Request,
        error: RequestValidationError,
    ):
        # Не повертаємо тіло запиту, адже воно може містити пароль.
        return JSONResponse(
            status_code=400,
            content={
                "error": {
                    "code": "VALIDATION_ERROR",
                    "message": "Перевірте структуру запиту та типи полів",
                }
            },
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_error_handler(
        request: Request,
        error: StarletteHTTPException,
    ):
        return JSONResponse(
            status_code=error.status_code,
            content={
                "error": {
                    "code": "HTTP_ERROR",
                    "message": "Ресурс або метод запиту недоступний",
                }
            },
            headers=error.headers,
        )

    @app.post("/auth/login")
    async def login(body: LoginRequest, request: Request):
        email = body.email.strip().lower()

        if not email or not body.password.strip():
            raise ApiError(
                400,
                "VALIDATION_ERROR",
                "Введіть email і пароль",
            )

        account = ACCOUNTS.get(email)

        # Порівнюємо початковий пароль без trim і зміни регістру.
        if account is None or body.password != account["password"]:
            raise ApiError(
                401,
                "INVALID_CREDENTIALS",
                "Неправильний email або пароль",
            )

        now = int(time.time())
        expires_at = now + request.app.state.ttl_seconds

        payload = {
            "sub": account["id"],
            "email": email,
            "name": account["name"],
            "role": account["role"],
            "iat": now,
            "exp": expires_at,
        }

        token = jwt.encode(
            payload,
            request.app.state.jwt_secret,
            algorithm="HS256",
        )

        return {
            "accessToken": token,
            "tokenType": "Bearer",
            "expiresAt": expires_at,
        }

    @app.get("/scenarios")
    async def list_scenarios(
        request: Request,
        user: dict = Depends(require_user),
    ):
        fault_response = await apply_fault(request)

        if fault_response is not None:
            return fault_response

        return request.app.state.catalog

    @app.get("/scenarios/{scenario_id}")
    async def get_scenario(
        scenario_id: str,
        request: Request,
        user: dict = Depends(require_user),
    ):
        scenario = next(
            (
                item
                for item in request.app.state.catalog["scenarios"]
                if item["id"] == scenario_id
            ),
            None,
        )

        if scenario is None:
            raise ApiError(
                404,
                "SCENARIO_NOT_FOUND",
                "Сценарій не знайдено",
            )

        fault_response = await apply_fault(request)

        if fault_response is not None:
            return fault_response

        return scenario

    @app.get("/admin/summary")
    async def admin_summary(
        request: Request,
        user: dict = Depends(require_admin),
    ):
        scenarios = request.app.state.catalog["scenarios"]

        return {
            "scenarioCount": len(scenarios),
            "devicePositionCount": sum(
                len(scenario["devices"]) for scenario in scenarios
            ),
        }

    return app


app = create_app(
    ttl_seconds=int(os.getenv("ENERGY_TOKEN_TTL_SECONDS", "3600")),
    fault=os.getenv("ENERGY_FAULT", "none"),
    secret=os.getenv("ENERGY_JWT_SECRET"),
)