# Первинні докази перевірки PW03

Версія реалізації: `701ab22d7d9eefa9ef12f652d81344d0bd079a2d`.
Цей каталог доповнює підсумок, а не замінює первинні результати переказом.

| Дата запуску | Перевірка | Первинні файли | Результат |
|---|---|---|---|
| 14.11.2026 | Android unit | [unit](2026-11-14/unit) — 22 XML | 145; failures/errors/skipped = 0 |
| 14.11.2026 | Android instrumented | [connected XML](2026-11-14/instrumented) | 30; failures/errors/skipped = 0 |
| 14.11.2026 | Lint | [XML](2026-11-14/lint-results-debug.xml), [HTML](2026-11-14/lint-results-debug.html) | 0 помилок, 12 попереджень |
| 16.11.2026 | Debug-збирання | [повний журнал](2026-11-16/build-debug.log) | BUILD SUCCESSFUL |
| 16.11.2026 | Серверний pytest | [JUnit XML](2026-11-16/server-pytest.xml), [журнал](2026-11-16/server-pytest.log) | 16 passed |

Android XML та Lint скопійовані без редагування зі збереженого `app/build`.
Код і конфігурація збігаються з зазначеним комітом; сам коміт не містив цих
первинних звітів. Перенесення доказів 16.11 не є повторним запуском Android-тестів.
Кількості включають успадковані та шаблонні тести.

Debug перевірено командою `gradlew.bat :app:assembleDebug --offline --console=plain`.
Це інкрементальне збирання з наявними кешами, не clean-build і не новий Lint.
Сервер перевірено командою `.venv/Scripts/python.exe -m pytest -q -p no:cacheprovider
--junitxml=../docs/evidence/2026-11-16/server-pytest.xml` у каталозі server.
Попередження Starlette зазначене в журналі; помилок тестів немає.

Файл [PW03_Report.docx](../PW03_Report.docx) побайтово відповідає наданому
PW03_Report_Final.docx. `manifest.json` містить SHA-256 звіту й кожного доказу.
Журнали можуть містити локальні шляхи середовища; токени та паролі до них не додавалися.
