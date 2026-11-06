import time

import jwt
import pytest
from fastapi.testclient import TestClient

from main import create_app


TEST_SECRET = "energy-forecast-test-key-only-" * 2


@pytest.fixture
def client():
    app = create_app(secret=TEST_SECRET)

    with TestClient(app) as test_client:
        yield test_client


def login_headers(client, admin=False):
    email = "admin@energy.test" if admin else "demo@energy.test"
    password = "Admin1234" if admin else "Demo1234"

    response = client.post(
        "/auth/login",
        json={"email": email, "password": password},
    )

    assert response.status_code == 200

    return {
        "Authorization": f"Bearer {response.json()['accessToken']}"
    }


@pytest.mark.parametrize(
    "email,password,role",
    [
        ("demo@energy.test", "Demo1234", "USER"),
        ("admin@energy.test", "Admin1234", "ADMIN"),
    ],
)
def test_login_returns_signed_token(client, email, password, role):
    response = client.post(
        "/auth/login",
        json={"email": email, "password": password},
    )

    assert response.status_code == 200
    data = response.json()

    payload = jwt.decode(
        data["accessToken"],
        TEST_SECRET,
        algorithms=["HS256"],
    )

    assert payload["role"] == role
    assert data["expiresAt"] == payload["exp"]
    assert data["tokenType"] == "Bearer"


def test_wrong_password(client):
    response = client.post(
        "/auth/login",
        json={"email": "demo@energy.test", "password": "wrong"},
    )

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_CREDENTIALS"
    assert "accessToken" not in response.json()


def test_empty_login(client):
    response = client.post(
        "/auth/login",
        json={"email": "", "password": ""},
    )

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "VALIDATION_ERROR"


@pytest.mark.parametrize(
    "headers,code",
    [
        ({}, "TOKEN_REQUIRED"),
        ({"Authorization": "Bearer broken-token"}, "INVALID_TOKEN"),
    ],
)
def test_catalog_requires_valid_token(client, headers, code):
    response = client.get("/scenarios", headers=headers)

    assert response.status_code == 401
    assert response.json()["error"]["code"] == code


def make_token(secret, expired=False):
    now = int(time.time())

    return jwt.encode(
        {
            "sub": "demo-user",
            "email": "demo@energy.test",
            "name": "Демо користувач",
            "role": "USER",
            "iat": now - 120,
            "exp": now - 60 if expired else now + 3600,
        },
        secret,
        algorithm="HS256",
    )


def test_expired_token(client):
    token = make_token(TEST_SECRET, expired=True)
    response = client.get(
        "/scenarios",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "TOKEN_EXPIRED"


def test_wrong_signature(client):
    token = make_token("another-test-signing-key-" * 2)
    response = client.get(
        "/scenarios",
        headers={"Authorization": f"Bearer {token}"},
    )

    assert response.status_code == 401
    assert response.json()["error"]["code"] == "INVALID_TOKEN"


def test_user_cannot_read_admin_summary(client):
    response = client.get(
        "/admin/summary",
        headers=login_headers(client),
    )

    assert response.status_code == 403
    assert response.json()["error"]["code"] == "FORBIDDEN"


def test_admin_summary(client):
    response = client.get(
        "/admin/summary",
        headers=login_headers(client, admin=True),
    )

    assert response.status_code == 200
    assert response.json() == {
        "scenarioCount": 3,
        "devicePositionCount": 8,
    }


def test_catalog_contains_nested_devices(client):
    response = client.get(
        "/scenarios",
        headers=login_headers(client),
    )

    assert response.status_code == 200
    scenarios = response.json()["scenarios"]

    assert len(scenarios) == 3
    assert len(scenarios[0]["devices"]) == 2


def test_scenario_details_and_control_consumption(client):
    response = client.get(
        "/scenarios/home-office",
        headers=login_headers(client),
    )

    assert response.status_code == 200
    scenario = response.json()
    assert scenario["id"] == "home-office"

    daily = sum(
        device["powerWatts"] / 1000
        * device["hoursPerDay"]
        * device["deviceCount"]
        for device in scenario["devices"]
    )

    assert daily == pytest.approx(0.53)
    assert daily * 30 == pytest.approx(15.90)


def test_unknown_scenario(client):
    response = client.get(
        "/scenarios/missing",
        headers=login_headers(client),
    )

    assert response.status_code == 404
    assert response.json()["error"]["code"] == "SCENARIO_NOT_FOUND"


def test_transient_failure_then_success():
    app = create_app(secret=TEST_SECRET, fault="transient503")

    with TestClient(app) as client:
        headers = login_headers(client)
        statuses = [
            client.get("/scenarios", headers=headers).status_code
            for _ in range(3)
        ]

    assert statuses == [503, 503, 200]


@pytest.mark.parametrize("fault", ["malformed", "invalid"])
def test_fault_modes_return_controlled_bad_data(fault):
    app = create_app(secret=TEST_SECRET, fault=fault)

    with TestClient(app) as client:
        response = client.get(
            "/scenarios",
            headers=login_headers(client),
        )

    assert response.status_code == 200

    if fault == "malformed":
        with pytest.raises(ValueError):
            response.json()
    else:
        device = response.json()["scenarios"][0]["devices"][0]
        assert device["powerWatts"] < 0