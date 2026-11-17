# Google Maps на фізичному Samsung Galaxy A54

Пристрій: SM-A546B, Android 16 / API 36. Google Maps завантажила справжні тайли з локальним ключем, який не публікується. Зображення отримуються через GoogleMap.snapshot у GoogleMapSdkTest/GoogleMapBrowserTest: це оригінальні результати SDK-рендерера production EnergyMap, а не макети. Перші три кадри — SDK test host; четвертий і п’ятий — справжні SiteBrowser + EnergyMap зі стабільними локальними вимірами у тестовому host. Це не ручне проходження MainActivity/NavHost.

| Файл | Зміст і межі доказу |
|---|---|
| [01-map-loaded.png](01-map-loaded.png) | Завантажена Google Maps із шістьма вигаданими об'єктами; карта Києва справжня, дані споживачів — демонстраційні |
| [02-marker-selected.png](02-marker-selected.png) | Реальне натискання office-2; блакитний маркер та власний багаторядковий InfoWindow з назвою, типом/станом, описом і показниками |
| [03-camera-refocused.png](03-camera-refocused.png) | Фінальний стан після повторного фокусу home-1; саму послідовність pan → нові дані без фокусу → повторна дія доводять assertions тесту, один PNG її не відтворює |
| [04-filtered-map.png](04-filtered-map.png) | Короткий слот мапи після Офіс + Активний + 30–50 кВт; один видимий об'єкт office-1. Вибір картки/кількість/скидання перевірено assertions GoogleMapBrowserTest, оскільки SDK snapshot не включає решту Compose-каталогу |

| [05-empty-map.png](05-empty-map.png) | Після reset → 6 і пошуку «Неіснуючий» → 0; SDK без маркерів споживачів, Compose-повідомлення перевірено assertions |

Розміри й SHA-256/UTC-час — у [manifest.json](manifest.json). Сирі SDK snapshots не ретушуються й не обрізаються скриптом. Зберігається атрибуція Google. Попередні діагностичні кадри із обрізаним стандартним snippet і замалим відступом залишаються лише в локальному build/verification; це не поточні результати.

## Відтворення

Зібрати debug APK і androidTest APK з локальним Maps-ключем та повним JDK. Підключити/розблокувати власний Android-пристрій або належно налаштований AVD, потім із PW04:

```powershell
python scripts/collect_maps_evidence.py --serial DEVICE_ADDRESS --adb "C:/Users/tinav/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

Скрипт установлює APK, запускає 4 тести у трьох класах (GoogleMapSdkTest 2, GoogleMapBrowserTest 1, MapInfoWindowTest 1), перевіряє OK (4 tests), копіює тільки files/pw04-maps-evidence з приватного каталогу тестованого застосунку та формує manifest. Для іншого ПК передати свій adb. Тестові вікна мають FLAG_KEEP_SCREEN_ON; системний таймаут і захист блокування не змінюються. Потрібен розблокований екран до старту. Новий збір замінює ці кадри актуальними даними.

Не доведено цими кадрами: фізичний GPS/зелений маркер, системні діалоги дозволів і відмови, обрив мережі/неправильний ключ/повтор мапи, усі критерії сортування на справжньому SDK та процес відновлення після process death. Детальні результати й незавершені перевірки — [verification.md](../../verification.md).

Додаткові системні й негативні сценарії вже зібрано окремо: [остаточний каталог](../final-checks/README.md), [підсумкова перевірка](../../final-checks.md). Попередній перелік меж описує саме ці SDK snapshots.
