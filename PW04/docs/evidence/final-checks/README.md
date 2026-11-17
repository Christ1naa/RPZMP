# Остаточний каталог доказів PW04

[Відкрити галерею з прев’ю](gallery.html).

Дата: 16.11.2026. A54/SM-A546B, Android 16/API 36; окремий Pixel_6 AVD/API 36 лише для unavailable services. Оригінальні PNG через UiAutomation.takeScreenshot або GoogleMap.snapshot, без ретушування/обрізання. Автоматичні дії й окремий огляд зображень; ручна демонстрація студенткою не заявляється.

Повний облік: [final-checks.md](../../final-checks.md), [результати методів](../../verification-results.json). Реальні приватні координати та ключ не зберігалися. Зелений маркер/accuracy/рух — позначені системні test providers Києва; реальний немокований network fix перевірено без знімка приватної позиції. Мережеві кадри — ізольований EnergyMap над тестовою ділянкою Рейк'явіка. Контрольовані UI-стани мають явний підпис.

## approximatePermissionThroughSystemDialog

Оригінали й SHA-256/UTC: [manifest.json](approximatePermissionThroughSystemDialog/manifest.json).

| Кадр | Що доводить |
|---|---|
| [11-permission-approximate-dialog](approximatePermissionThroughSystemDialog/pw04-final-evidence--11-permission-approximate-dialog.png) | Справжній діалог: Approximate; coarse без fine підтверджено assertions. |

## controlled-states

Оригінали й SHA-256/UTC: [manifest.json](controlled-states/manifest.json).

| Кадр | Що доводить |
|---|---|
| [80-controlled-waiting](controlled-states/pw04-final-evidence--80-controlled-waiting.png) | Позначений контрольований UI-стан: WAITING/немає даних, не нульовий прогноз. |
| [81-controlled-failed](controlled-states/pw04-final-evidence--81-controlled-failed.png) | Позначений контрольований UI-стан: FAILED, останні дані застарілі, ручне відновлення. |
| [82-controlled-unknown-id](controlled-states/pw04-final-evidence--82-controlled-unknown-id.png) | Позначений контрольований UI-стан: невідомий ID. |
| [83-controlled-empty-dataset](controlled-states/pw04-final-evidence--83-controlled-empty-dataset.png) | Позначений контрольований UI-стан: порожня колекція, не нуль пошукових збігів. |

## denyTwiceThenOpenOwnSettingsAndReturn

Оригінали й SHA-256/UTC: [manifest.json](denyTwiceThenOpenOwnSettingsAndReturn/manifest.json).

| Кадр | Що доводить |
|---|---|
| [12-permission-denial-dialog](denyTwiceThenOpenOwnSettingsAndReturn/pw04-final-evidence--12-permission-denial-dialog.png) | Системний діалог перед першою відмовою. |
| [13-permission-denied-app](denyTwiceThenOpenOwnSettingsAndReturn/pw04-final-evidence--13-permission-denied-app.png) | Застосунок після першої відмови; список і Maps доступні. |
| [14-permission-permanently-denied-app](denyTwiceThenOpenOwnSettingsAndReturn/pw04-final-evidence--14-permission-permanently-denied-app.png) | Після двох відмов/третього запиту; відсутність нового діалогу перевірена assertions. |
| [15-own-app-settings](denyTwiceThenOpenOwnSettingsAndReturn/pw04-final-evidence--15-own-app-settings.png) | Системний App info саме PW04, перед поверненням у застосунок. |

## general-absent

Оригінали й SHA-256/UTC: [manifest.json](general-absent/manifest.json).

| Кадр | Що доводить |
|---|---|
| [20-meter-retrying](general-absent/pw04-final-evidence--20-meter-retrying.png) | Збій лічильника: RETRYING, останній прогноз застарілий. |
| [20-model-retrying](general-absent/pw04-final-evidence--20-model-retrying.png) | Збій моделі: RETRYING, лічильник продовжує роботу. |
| [21-meter-recovered](general-absent/pw04-final-evidence--21-meter-recovered.png) | Лічильник автоматично відновився, обидва джерела LIVE. |
| [21-model-recovered](general-absent/pw04-final-evidence--21-model-recovered.png) | Модель автоматично відновилася, обидва джерела LIVE. |
| [22-invalid-range](general-absent/pw04-final-evidence--22-invalid-range.png) | Мінімум 50 перевищує максимум 10; видима помилка валідації. |
| [23-sort-date-asc](general-absent/pw04-final-evidence--23-sort-date-asc.png) | Сортування date за зростанням. |
| [23-sort-date-desc](general-absent/pw04-final-evidence--23-sort-date-desc.png) | Сортування date за спаданням. |
| [23-sort-forecast-asc](general-absent/pw04-final-evidence--23-sort-forecast-asc.png) | Сортування forecast за зростанням. |
| [23-sort-forecast-desc](general-absent/pw04-final-evidence--23-sort-forecast-desc.png) | Сортування forecast за спаданням. |
| [23-sort-name-asc](general-absent/pw04-final-evidence--23-sort-name-asc.png) | Сортування name за зростанням. |
| [23-sort-name-desc](general-absent/pw04-final-evidence--23-sort-name-desc.png) | Сортування name за спаданням. |
| [23-sort-power-asc](general-absent/pw04-final-evidence--23-sort-power-asc.png) | Сортування power за зростанням. |
| [23-sort-power-desc](general-absent/pw04-final-evidence--23-sort-power-desc.png) | Сортування power за спаданням. |
| [23-sort-status-asc](general-absent/pw04-final-evidence--23-sort-status-asc.png) | Сортування status за зростанням. |
| [23-sort-status-desc](general-absent/pw04-final-evidence--23-sort-status-desc.png) | Сортування status за спаданням. |
| [23-sort-type-asc](general-absent/pw04-final-evidence--23-sort-type-asc.png) | Сортування type за зростанням. |
| [23-sort-type-desc](general-absent/pw04-final-evidence--23-sort-type-desc.png) | Сортування type за спаданням. |
| [24-landscape](general-absent/pw04-final-evidence--24-landscape.png) | Справжній landscape Configuration, карта прихована, список прокручено. |
| [25-portrait-restored](general-absent/pw04-final-evidence--25-portrait-restored.png) | Повернення у portrait після повороту; стан каталогу збережено. |
| [01-monitor-live](general-absent/pw04-evidence--01-monitor-live.png) | Моніторинг: обидва джерела LIVE, потужність і добовий прогноз. |
| [02-catalog-no-key](general-absent/pw04-evidence--02-catalog-no-key.png) | Каталог без ключа: інструкція замість Maps, список доступний. |
| [03-filter-panel](general-absent/pw04-evidence--03-filter-panel.png) | Комбіновані фільтри: Офіс + Активний + 30–50 кВт. |
| [04-filtered-office](general-absent/pw04-evidence--04-filtered-office.png) | Один office-1 після комбінованого запиту. |
| [05-details-top](general-absent/pw04-evidence--05-details-top.png) | Деталі об'єкта: поля, одиниці, дати, демонстраційні координати. |
| [06-details-bottom](general-absent/pw04-evidence--06-details-bottom.png) | Деталі після команди прокрутки; окремого зсуву, якщо все вмістилося, не доводить. |
| [07-empty-search](general-absent/pw04-evidence--07-empty-search.png) | Нуль збігів за пошуком і наявними фільтрами; доступне скидання. |

## general-valid

Оригінали й SHA-256/UTC: [manifest.json](general-valid/manifest.json).

| Кадр | Що доводить |
|---|---|
| [20-meter-retrying](general-valid/pw04-final-evidence--20-meter-retrying.png) | Збій лічильника: RETRYING, останній прогноз застарілий. |
| [20-model-retrying](general-valid/pw04-final-evidence--20-model-retrying.png) | Збій моделі: RETRYING, лічильник продовжує роботу. |
| [21-meter-recovered](general-valid/pw04-final-evidence--21-meter-recovered.png) | Лічильник автоматично відновився, обидва джерела LIVE. |
| [21-model-recovered](general-valid/pw04-final-evidence--21-model-recovered.png) | Модель автоматично відновилася, обидва джерела LIVE. |
| [22-invalid-range](general-valid/pw04-final-evidence--22-invalid-range.png) | Мінімум 50 перевищує максимум 10; видима помилка валідації. |
| [23-sort-date-asc](general-valid/pw04-final-evidence--23-sort-date-asc.png) | Сортування date за зростанням. |
| [23-sort-date-desc](general-valid/pw04-final-evidence--23-sort-date-desc.png) | Сортування date за спаданням. |
| [23-sort-forecast-asc](general-valid/pw04-final-evidence--23-sort-forecast-asc.png) | Сортування forecast за зростанням. |
| [23-sort-forecast-desc](general-valid/pw04-final-evidence--23-sort-forecast-desc.png) | Сортування forecast за спаданням. |
| [23-sort-name-asc](general-valid/pw04-final-evidence--23-sort-name-asc.png) | Сортування name за зростанням. |
| [23-sort-name-desc](general-valid/pw04-final-evidence--23-sort-name-desc.png) | Сортування name за спаданням. |
| [23-sort-power-asc](general-valid/pw04-final-evidence--23-sort-power-asc.png) | Сортування power за зростанням. |
| [23-sort-power-desc](general-valid/pw04-final-evidence--23-sort-power-desc.png) | Сортування power за спаданням. |
| [23-sort-status-asc](general-valid/pw04-final-evidence--23-sort-status-asc.png) | Сортування status за зростанням. |
| [23-sort-status-desc](general-valid/pw04-final-evidence--23-sort-status-desc.png) | Сортування status за спаданням. |
| [23-sort-type-asc](general-valid/pw04-final-evidence--23-sort-type-asc.png) | Сортування type за зростанням. |
| [23-sort-type-desc](general-valid/pw04-final-evidence--23-sort-type-desc.png) | Сортування type за спаданням. |
| [24-landscape](general-valid/pw04-final-evidence--24-landscape.png) | Справжній landscape Configuration, карта прихована, список прокручено. |
| [25-portrait-restored](general-valid/pw04-final-evidence--25-portrait-restored.png) | Повернення у portrait після повороту; стан каталогу збережено. |
| [01-map-loaded](general-valid/pw04-maps-evidence--01-map-loaded.png) | SDK snapshot: справжні тайли та 6 демонстраційних маркерів. |
| [02-marker-selected](general-valid/pw04-maps-evidence--02-marker-selected.png) | SDK snapshot: вибраний блакитний marker та багаторядковий InfoWindow. |
| [03-camera-refocused](general-valid/pw04-maps-evidence--03-camera-refocused.png) | SDK snapshot: кінцевий стан після повторного фокусу; послідовність доводять assertions. |
| [04-filtered-map](general-valid/pw04-maps-evidence--04-filtered-map.png) | SDK snapshot: один office-1 після Офіс + Активний + 30–50 кВт. |
| [05-empty-map](general-valid/pw04-maps-evidence--05-empty-map.png) | SDK snapshot: пошук дав 0, маркери споживачів видалено; це не весь Compose-екран. |

## invalid-key-retry

Оригінали й SHA-256/UTC: [manifest.json](invalid-key-retry/manifest.json).

| Кадр | Що доводить |
|---|---|
| [40-invalid-key-map-failed](invalid-key-retry/pw04-final-evidence--40-invalid-key-map-failed.png) | Навмисно неправильний ключ у MainActivity: timeout і Повторити мапу. |
| [41-invalid-key-map-retrying](invalid-key-retry/pw04-final-evidence--41-invalid-key-map-retrying.png) | Після Повторити мапу створено нову генерацію, показано завантаження. |
| [42-invalid-key-catalogue-still-usable](invalid-key-retry/pw04-final-evidence--42-invalid-key-catalogue-still-usable.png) | При неправильному ключі каталог/деталі залишаються доступними. |

## location-disabled-settings

Оригінали й SHA-256/UTC: [manifest.json](location-disabled-settings/manifest.json).

| Кадр | Що доводить |
|---|---|
| [17-location-disabled](location-disabled-settings/pw04-final-evidence--17-location-disabled.png) | Системна Location вимкнена; наступне повернення із GPS settings перевірено тестом. |

## network-recovery

Оригінали й SHA-256/UTC: [manifest.json](network-recovery/manifest.json).

| Кадр | Що доводить |
|---|---|
| [50-network-off-map-failed](network-recovery/50-network-off-map-failed.png) | Реальні Wi-Fi/mobile data вимкнено; ізольований EnergyMap, timeout. |
| [51-network-retry-loading](network-recovery/51-network-retry-loading.png) | Повтор завантаження до повернення мережі, ізольований EnergyMap. |
| [52-network-restored-map-loaded](network-recovery/52-network-restored-map-loaded.png) | Мережа відновлена: справжні тайли тестової ділянки Рейк'явіка, не приватне місцезнаходження. |

## precisePermissionThroughSystemDialog

Оригінали й SHA-256/UTC: [manifest.json](precisePermissionThroughSystemDialog/manifest.json).

| Кадр | Що доводить |
|---|---|
| [10-permission-precise-dialog](precisePermissionThroughSystemDialog/pw04-final-evidence--10-permission-precise-dialog.png) | Справжній системний діалог A54: Precise, до надання дозволу. |

## real-device-fix

Оригінали й SHA-256/UTC: [manifest.json](real-device-fix/manifest.json).

Цей сценарій не зберігає кадри: реальна приватна позиція залишається на пристрої; adapter-сценарій перевіряє callback-контракт. Успіх підтверджений AndroidJUnitRunner.

## revokedPermissionIsRecheckedOnNewActivity

Оригінали й SHA-256/UTC: [manifest.json](revokedPermissionIsRecheckedOnNewActivity/manifest.json).

| Кадр | Що доводить |
|---|---|
| [16-permission-revoked-app](revokedPermissionIsRecheckedOnNewActivity/pw04-final-evidence--16-permission-revoked-app.png) | Дозвіл grant → revoke до запуску нової Activity; повідомлення без аварії. |

## services-unavailable-emulator

Оригінали й SHA-256/UTC: [manifest.json](services-unavailable-emulator/manifest.json).

| Кадр | Що доводить |
|---|---|
| [60-play-services-unavailable](services-unavailable-emulator/60-play-services-unavailable.png) | Окремий AVD із тимчасово вимкненими GMS: чесне повідомлення й доступний каталог. |
| [61-services-unavailable-details-work](services-unavailable-emulator/61-services-unavailable-details-work.png) | Деталі працюють на AVD без GMS; стан services відновлено після тесту. |

## synthetic-position-ui

Оригінали й SHA-256/UTC: [manifest.json](synthetic-position-ui/manifest.json).

| Кадр | Що доводить |
|---|---|
| [30-labelled-synthetic-position](synthetic-position-ui/pw04-final-evidence--30-labelled-synthetic-position.png) | Свіжа тестова позиція ±5 м; це початковий кадр, тайли можуть ще завантажуватися. |
| [31-labelled-position-updated](synthetic-position-ui/pw04-final-evidence--31-labelled-position-updated.png) | Другий системний mock fix Києва ±6 м і зелений маркер, явно тестова позиція. |
| [32-providers-disabled](synthetic-position-ui/pw04-final-evidence--32-providers-disabled.png) | Обидва тестові провайдери вимкнено; актуальна позиція очищена. |
| [33-providers-recovered](synthetic-position-ui/pw04-final-evidence--33-providers-recovered.png) | Після повторного увімкнення на тих самих координатах: ±7 м і зелений маркер. |

## system-gps-adapter

Оригінали й SHA-256/UTC: [manifest.json](system-gps-adapter/manifest.json).

Цей сценарій не зберігає кадри: реальна приватна позиція залишається на пристрої; adapter-сценарій перевіряє callback-контракт. Успіх підтверджений AndroidJUnitRunner.

## system-large-font

Оригінали й SHA-256/UTC: [manifest.json](system-large-font/manifest.json).

| Кадр | Що доводить |
|---|---|
| [26-system-large-font](system-large-font/pw04-final-evidence--26-system-large-font.png) | Системний font_scale=1.6, список прокручується до останнього об'єкта. |
| [27-large-font-filter-reset](system-large-font/pw04-final-evidence--27-large-font-filter-reset.png) | Великий системний шрифт: доступна дія Скинути. |
| [28-large-font-filter-done](system-large-font/pw04-final-evidence--28-large-font-filter-done.png) | Великий системний шрифт: доступна дія Готово. |

## Окремі SDK snapshots і історія

П'ять актуальних [SDK snapshots](../maps-phone/README.md) мають власний manifest і є частково повторними доказами тих самих Maps-сценаріїв. Початкові [сім кадрів етапу 8](../README.md) збережені окремо як історія; це не додаткові унікальні тести.

У цьому каталозі **77 оригінальних PNG**; повторні конфігурації мають окремі кадри, тому кількість файлів не дорівнює кількості унікальних сценаріїв. Попередні невдалі й перехідні діагностичні кадри зберігаються лише локально в ignored build/verification. Файл 30 може бути початковим loading-кадром; для видимого зеленого маркера використовувати 31/33.

Динамічні виміри між кадрами можуть змінюватися. Команда прокрутки не гарантує зсуву, якщо весь вміст уже вмістився. Зображення не доводить послідовність взаємодій самостійно: її перевіряють assertions відповідного тесту. Process death, TalkBack, foldable й супутниковий GNSS цими кадрами не підтверджуються.
