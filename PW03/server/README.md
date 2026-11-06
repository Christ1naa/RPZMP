# Навчальний сервер EnergyForecast

Сервер для практичної роботи №3.
Працює локально й використовує вигадані навчальні акаунти.

## Середовище

PowerShell, робоча тека:
C:\Projects\RPZMP\PW03\server

Створення середовища:

```powershell
py -3.13 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

За наявності requirements-lock.txt точні встановлені версії
можна відновити з нього замість requirements.txt.

## Тести

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

## Запуск

Перед першим запуском у вікні PowerShell:

```powershell
$env:ENERGY_JWT_SECRET = (& .\.venv\Scripts\python.exe -c "import secrets; print(secrets.token_urlsafe(32))").Trim()
$env:ENERGY_TOKEN_TTL_SECONDS = "3600"
$env:ENERGY_FAULT = "none"
```

Запуск:

```powershell
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 5156
```

Зупинка: Ctrl+C.

Документація API:
http://127.0.0.1:5156/docs

Якщо ключ не заданий, сервер створює тимчасовий.
Зміна ключа робить раніше видані токени недійсними.

## Навчальні акаунти

- demo@energy.test / Demo1234 — USER.
- admin@energy.test / Admin1234 — ADMIN.

Реєстрація не реалізована.

## Endpoint

- POST /auth/login
- GET /scenarios
- GET /scenarios/{id}
- GET /admin/summary

Усі ресурси, крім входу, потребують Bearer-токена.
Адміністративний ресурс доступний лише ADMIN.

## Керовані збої

Після зупинки сервера змінити ENERGY_FAULT та запустити знову.

| Режим | Поведінка GET сценаріїв |
|---|---|
| none | Звичайна відповідь |
| transient503 | Перші два запити до конкретного шляху повертають 503, наступні — звичайну відповідь |
| always503 | Кожний запит повертає 503 |
| malformed | HTTP 200 із пошкодженим JSON |
| invalid | HTTP 200 із від’ємною потужністю приладу |
| timeout | Затримка відповіді на 30 секунд |

Приклад:

```powershell
$env:ENERGY_FAULT = "transient503"
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 5156
```

Лічильники запитів скидаються після перезапуску.
Авторизація перевіряється перед застосуванням збою.
Режими не змінюють дані fixtures/scenarios.json.

Для перевірки завершення сесії можна встановити
ENERGY_TOKEN_TTL_SECONDS у 10 перед запуском.
Після перевірки повернути 3600.

Для звичайної роботи повернути ENERGY_FAULT у none.

## Межі

Сервер не містить постійної бази користувачів.
Каталог завантажується з fixtures/scenarios.json.
Персональні прилади та історія Android на сервер не передаються.
Ключ підпису не додається в Android-код і Git.