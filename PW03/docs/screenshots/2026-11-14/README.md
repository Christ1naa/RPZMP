# Скриншоти до звіту ПР3

Знімки отримано з реального debug-застосунку на Pixel_6 AVD (Android 16), 14.11.2026.
Оригінальні PNG не обрізано й не ретушовано: 1080×2400 або 2400×1080.
Час у статусному рядку та застосунку — GMT (час емулятора); Europe/Amsterdam цього дня на годину попереду.
Використано лише вигадані навчальні облікові записи. Паролі на екрані замасковані; токенів у матеріалах немає.

Контрольний сценарій має два прилади і дає 0,53 кВт·год/добу та 15,90 кВт·год за 30 днів.
На графіку 18 додатково є вентилятор 45 Вт × 4 год: разом 0,71 кВт·год/добу та 21,30 кВт·год за 30 днів.
Це різні набори приладів, а збережена історія містить параметри на момент збереження.

Для офлайн-перевірок на тимчасовому емуляторі вимикалися Wi-Fi і мобільні дані.
Для помилок використано наявні режими навчального сервера: transient503, always503, invalid, malformed, timeout.
Тимчасова обгортка сервера лише перемикала ці режими й TTL у процесі; реалізацію endpoint не змінено.
Для скриншота 43 видано токен на 15 секунд; після перевірки повернуто 3600 секунд і ENERGY_FAULT=none.
Для скриншота 44 очищено лише тестові дані застосунку в тимчасовому read-only емуляторі.

Під час знімання виправлено пропущений повтор GET після транспортного IOException.
У каталозі diagnostics-before-fix збережено відхилені діагностичні/перехідні кадри; вони не входять до ZIP для звіту.
Основні екрани, зняті до цієї зміни, мають той самий UI; мережеві результати 36–47 перевірено після виправлення.

## Рекомендована послідовність рисунків

| Рисунок | Файл | Готовий підпис |
|---|---|---|
| 1 | [01_login.png](01_login.png) | Екран входу до застосунку EnergyForecast. |
| 2 | [02_login_validation.png](02_login_validation.png) | Валідація порожніх полів email і пароля. |
| 3 | [03_invalid_credentials.png](03_invalid_credentials.png) | Повідомлення про неправильні облікові дані. |
| 4 | [07_catalog_refresh_success.png](07_catalog_refresh_success.png) | Успішне завантаження каталогу з API та час оновлення кешу. |
| 5 | [08_catalog_office_filter.png](08_catalog_office_filter.png) | Фільтрація каталогу за категорією «Офіс»: один сценарій із трьох. |
| 6 | [09_scenario_forecast.png](09_scenario_forecast.png) | Контрольний прогноз сценарію «Домашній офіс»: 0,53 кВт·год на добу та 15,90 кВт·год за 30 днів. |
| 7 | [12_import_success.png](12_import_success.png) | Успішний імпорт приладів сценарію до персонального профілю. |
| 8 | [15_device_validation.png](15_device_validation.png) | Валідація обов’язкових параметрів нового приладу. |
| 9 | [18_consumption_chart.png](18_consumption_chart.png) | Графік накопиченого споживання трьох приладів: 21,30 кВт·год за 30 днів. |
| 10 | [48_forecast_history_verified.png](48_forecast_history_verified.png) | Історія збережених прогнозів: контрольний прогноз за 30 днів. |
| 11 | [30_admin_access.png](30_admin_access.png) | Доступна лише ADMIN секція адміністрування. |
| 12 | [33_user_menu_without_admin.png](33_user_menu_without_admin.png) | Меню USER після зміни ролі: секція адміністрування відсутня. |
| 13 | [42_admin_statistics_verified.png](42_admin_statistics_verified.png) | Захищена адміністративна статистика: три сценарії та вісім позицій приладів. |
| 14 | [24_offline_cache_error.png](24_offline_cache_error.png) | Відсутність активної мережі: повідомлення про помилку та збережений каталог. |
| 15 | [44_offline_empty_cache.png](44_offline_empty_cache.png) | Відсутність мережі та кешу: пояснення помилки й порожній стан каталогу. |
| 16 | [46_offline_persistent_catalog.png](46_offline_persistent_catalog.png) | Каталог Room доступний після повного перезапуску застосунку без мережі. |
| 17 | [47_adaptive_landscape_cards.png](47_adaptive_landscape_cards.png) | Адаптивні картки каталогу у двох колонках у горизонтальній орієнтації. |
| 18 | [36_retry_recovery_verified.png](36_retry_recovery_verified.png) | Успішне оновлення після автоматичних повторів: HTTP 503 → HTTP 503 → успіх. |
| 19 | [37_server_503_preserves_cache.png](37_server_503_preserves_cache.png) | Постійна помилка HTTP 503: обмежені спроби завершено, попередній кеш збережено. |
| 20 | [38_invalid_api_data.png](38_invalid_api_data.png) | Відхилення невалідних значень API без заміни попереднього коректного кешу. |
| 21 | [40_timeout_preserves_cache.png](40_timeout_preserves_cache.png) | Завершення обмежених спроб після timeout, повідомлення та незмінений кеш. |
| 22 | [43_token_expired.png](43_token_expired.png) | Автоматичне завершення сесії після закінчення строку дії тестового токена. |
| 23 | [35_session_invalidated.png](35_session_invalidated.png) | Завершення сесії після серверного HTTP 401 та запрошення повторного входу. |

## Повний перелік придатних кадрів

| ID | Файл | Зміст |
|---|---|---|
| 01 | [01_login.png](01_login.png) | Екран входу до застосунку EnergyForecast. |
| 02 | [02_login_validation.png](02_login_validation.png) | Валідація порожніх полів email і пароля. |
| 03 | [03_invalid_credentials.png](03_invalid_credentials.png) | Повідомлення про неправильні облікові дані. |
| 04 | [04_user_menu.png](04_user_menu.png) | Головне меню авторизованого користувача USER. |
| 05 | [05_catalog_empty.png](05_catalog_empty.png) | Початковий порожній каталог до першого завантаження з API. |
| 07 | [07_catalog_refresh_success.png](07_catalog_refresh_success.png) | Успішне завантаження каталогу з API та час оновлення кешу. |
| 08 | [08_catalog_office_filter.png](08_catalog_office_filter.png) | Фільтрація каталогу за категорією «Офіс»: один сценарій із трьох. |
| 09 | [09_scenario_forecast.png](09_scenario_forecast.png) | Контрольний прогноз сценарію «Домашній офіс»: 0,53 кВт·год на добу та 15,90 кВт·год за 30 днів. |
| 10 | [10_scenario_devices.png](10_scenario_devices.png) | Вкладений список приладів сценарію: ноутбук і лампа. |
| 11 | [11_invalid_forecast_period.png](11_invalid_forecast_period.png) | Відхилення періоду прогнозування 366 днів, що перевищує допустимий діапазон. |
| 12 | [12_import_success.png](12_import_success.png) | Успішний імпорт приладів сценарію до персонального профілю. |
| 13 | [13_profile_forecast.png](13_profile_forecast.png) | Прогноз персонального профілю після імпорту двох приладів. |
| 14 | [14_save_forecast_dialog.png](14_save_forecast_dialog.png) | Діалог збереження прогнозу з назвою для історії. |
| 15 | [15_device_validation.png](15_device_validation.png) | Валідація обов’язкових параметрів нового приладу. |
| 16 | [16_device_editor.png](16_device_editor.png) | Заповнена форма приладу: 45 Вт, 4 години на добу, одна одиниця. |
| 17 | [17_profile_devices.png](17_profile_devices.png) | Персональний список приладів із діями редагування та видалення. |
| 18 | [18_consumption_chart.png](18_consumption_chart.png) | Графік накопиченого споживання трьох приладів: 21,30 кВт·год за 30 днів. |
| 20 | [20_saved_forecast_details.png](20_saved_forecast_details.png) | Деталі збереженого прогнозу та параметри приладів на момент збереження. |
| 21 | [21_catalog_sort_options.png](21_catalog_sort_options.png) | Доступні режими сортування каталогу. |
| 22 | [22_catalog_sorted.png](22_catalog_sorted.png) | Каталог, відсортований за спаданням добового споживання: 1,97; 0,88; 0,53 кВт·год. |
| 23 | [23_search_empty_result.png](23_search_empty_result.png) | Порожній результат пошуку без зміни збереженого каталогу. |
| 24 | [24_offline_cache_error.png](24_offline_cache_error.png) | Відсутність активної мережі: повідомлення про помилку та збережений каталог. |
| 26 | [26_catalog_landscape.png](26_catalog_landscape.png) | Каталог у горизонтальній орієнтації: заголовок, оновлення та пошук. |
| 28 | [28_logout.png](28_logout.png) | Екран входу після виходу з облікового запису. |
| 29 | [29_admin_menu.png](29_admin_menu.png) | Головне меню облікового запису ADMIN. |
| 30 | [30_admin_access.png](30_admin_access.png) | Доступна лише ADMIN секція адміністрування. |
| 32 | [32_admin_separate_profile.png](32_admin_separate_profile.png) | Окремий порожній персональний профіль ADMIN без приладів користувача USER. |
| 33 | [33_user_menu_without_admin.png](33_user_menu_without_admin.png) | Меню USER після зміни ролі: секція адміністрування відсутня. |
| 35 | [35_session_invalidated.png](35_session_invalidated.png) | Завершення сесії після серверного HTTP 401 та запрошення повторного входу. |
| 36 | [36_retry_recovery_verified.png](36_retry_recovery_verified.png) | Успішне оновлення після автоматичних повторів: HTTP 503 → HTTP 503 → успіх. |
| 37 | [37_server_503_preserves_cache.png](37_server_503_preserves_cache.png) | Постійна помилка HTTP 503: обмежені спроби завершено, попередній кеш збережено. |
| 38 | [38_invalid_api_data.png](38_invalid_api_data.png) | Відхилення невалідних значень API без заміни попереднього коректного кешу. |
| 39 | [39_malformed_json.png](39_malformed_json.png) | Обробка пошкодженого JSON із повідомленням користувачу та збереженням кешу. |
| 40 | [40_timeout_preserves_cache.png](40_timeout_preserves_cache.png) | Завершення обмежених спроб після timeout, повідомлення та незмінений кеш. |
| 41 | [41_server_recovery.png](41_server_recovery.png) | Успішне відновлення оновлення каталогу після усунення серверного збою. |
| 42 | [42_admin_statistics_verified.png](42_admin_statistics_verified.png) | Захищена адміністративна статистика: три сценарії та вісім позицій приладів. |
| 43 | [43_token_expired.png](43_token_expired.png) | Автоматичне завершення сесії після закінчення строку дії тестового токена. |
| 44 | [44_offline_empty_cache.png](44_offline_empty_cache.png) | Відсутність мережі та кешу: пояснення помилки й порожній стан каталогу. |
| 45 | [45_catalog_online_restored.png](45_catalog_online_restored.png) | Перший успішний онлайн-запит після перевірки порожнього офлайн-кешу. |
| 46 | [46_offline_persistent_catalog.png](46_offline_persistent_catalog.png) | Каталог Room доступний після повного перезапуску застосунку без мережі. |
| 47 | [47_adaptive_landscape_cards.png](47_adaptive_landscape_cards.png) | Адаптивні картки каталогу у двох колонках у горизонтальній орієнтації. |
| 48 | [48_forecast_history_verified.png](48_forecast_history_verified.png) | Історія збережених прогнозів: контрольний прогноз за 30 днів. |

## Додаткові докази

- [Журнал мережевих запитів](network-evidence.txt): лише операція, спроба, тип результату, HTTP-код і тривалість.
- [Підсумок автоматичних перевірок](validation-summary.txt).
- [Маніфест PNG із SHA-256](manifest.json).
- [Галерея для перегляду](index.html).

У журналі видно 503 → 503 → SUCCESS. Для інших сценаріїв окремій відповіді сервера може передувати IOException застарілого з’єднання; загальна межа — три спроби. Після помилки валідації/JSON повтор не виконується.
HTTP 403/404 додатково підтверджено автоматичними API-тестами. Екран деталей читає кеш, тому окремий UI-кадр 404 не заявляється.
