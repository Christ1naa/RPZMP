# Перевірка PW04

## Актуальний підсумок — 16.11.2026

**54 JVM-тести та 64 унікальні Android-методи успішні в кількох конфігураціях; Debug і androidTest APK зібрано; Lint 0 помилок / 19 попереджень.** Окремо пройшли неправильний ключ, реальна втрата/відновлення мережі, відсутні Play services на AVD, системні діалоги/налаштування, реальний network fix і позначені синтетичні позиції. Кадри отримано на A54/API 36 і Pixel_6/API 36. Точний облік, відтворення, виправлення й межі — [final-checks.md](final-checks.md); [машинний перелік](verification-results.json); [остаточна галерея](evidence/final-checks/README.md).

Далі — історія попередніх етапів і запусків. Формулювання «відкладено», «не перевіряли», «скриншотів немає» відносяться до зазначених дат/наборів; вони не є поточним списком незавершеного. Невдалі спроби збережено, а не зараховано як успішні.

Дата: 11.11.2026. Перевіряється новий проєкт PW04, application ID `ua.kpi.energyforecast.pw04`. Результати PW03 сюди не переносилися.

Оновлення 16.11.2026: на фізичному SM-A546B успішні 4 цільові Maps/UI-тести; після виправлень InfoWindow й padding також успішні 54 unit, Lint 0/19 і збірки. Оригінальні SDK snapshots є в evidence/maps-phone. Невдалий AVD та аномалію Gradle connected task описано окремо.

Додаток етапу 8: окремий EvidenceCaptureTest успішний, отримано 7 справжніх PNG; див. секцію етапу 8 і evidence/README.md.

Перевірка етапу 7 (13.11.2026): **54 unit + 46 інструментальних тестів успішні, збірки успішні, Lint 0 помилок / 19 попереджень**. Два справжні Maps SDK-сценарії залишаються відкладеними. Деталі та попередні невдалі спроби — у секції етапу 7 нижче.

Попередня програмна перевірка етапу 5: **54 unit; доступний без ключа набір — 31 інструментальний тест; 2 SDK-сценарії заблоковані відсутністю ключа; збірки успішні; Lint 0 помилок / 22 попередження**. Етап 5 загалом не закрито. Першу спробу з 33 тестами та двома failure/AssumptionViolatedException описано окремо нижче; її не називаємо успішною перевіркою реальної мапи. Раніші етапи збережено як історію.

## Успішна Google Maps на фізичному Samsung — 16.11.2026

Пристрій SM-A546B (Galaxy A54), Android 16 / API 36, підключений через adb wireless pairing після явних дій користувача. IP/порт та pairing code не додаються до публічної документації. Локальний API-ключ виключений із Git.

1. Початковий `:app:connectedDebugAndroidTest` з class=GoogleMapSdkTest фактично виконав 2 тести на телефоні. XML: **2 tests / 0 failures / 0 errors / 0 skipped**, TestRunner: 2/2. Однак Gradle повернув BUILD FAILED (52 с), а test-result-exit-code.txt — 1. Причина підсумкового статусу не встановлена; не заявляється успішний connected task. XML збережено в ігнорованому build/verification/maps-phone-sdk.xml.
2. Прямий `adb shell am instrument -w -r -e class ...GoogleMapSdkTest ...AndroidJUnitRunner` повторно підтвердив **OK (2 tests)**, час 10,069 с. Лог — maps-phone-direct-runner.txt. Перші отримані кадри виявили обрізання стандартного snippet InfoWindow; це дефект, знайдений візуально на справжньому SDK, попри успішні assertions камери/маркерів.
3. Додано MarkerInfoWindow/SiteInfoWindow із багаторядковим описом і показниками; помилку сигнатури MarkerInfoWindowContent під час першої компіляції виправлено використанням MarkerInfoWindow, який підтримує contentDescription. Нові MapInfoWindowTest і GoogleMapBrowserTest перевіряють перенесення тексту та справжню інтеграцію каталогу/SDK.
4. Одну спробу нового набору перервано після діагностики `mWakefulness=Dozing`: агент зупинив лише PW04 через am force-stop, користувач розблокував телефон. Лог збережений у maps-phone-screen-asleep.txt; ця спроба не зарахована як успішна. Тестові вікна отримали FLAG_KEEP_SCREEN_ON, системні timeout/lock settings не змінювалися.
5. Після успішних 4 тестів SDK snapshot короткого слоту показав обрізаний заголовок popup. Додано адаптивний contentPadding зверху — min(96 dp, 40% висоти мапи). Це залишає місце над центром камери й не задає padding більший за viewport. Кадр до виправлення — лише build/verification/maps-filtered-before-padding.png.
6. Фінально після padding виконано **OK (4 tests)** через scripts/collect_maps_evidence.py: GoogleMapSdkTest 2, GoogleMapBrowserTest 1, MapInfoWindowTest 1. INSTRUMENTATION_CODE=-1 — нормальне завершення AndroidJUnitRunner. Сирий лог — build/verification/maps-phone-final-runner.txt. Усі 4 сценарії завершилися успішно; це цільовий набір, не новий повний запуск усіх instrumented-тестів.

Фінальна збірка/перевірка production-виправлення:

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain `
  '-Porg.gradle.java.installations.paths=C:/Program Files/Android/Android Studio/jbr' `
  '-Dorg.gradle.java.home=C:/Program Files/Android/Android Studio/jbr' --no-daemon
python scripts/collect_maps_evidence.py --serial DEVICE_ADDRESS --adb "C:/Users/tinav/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

Gradle BUILD SUCCESSFUL за 51 с (85 задач, 14 виконано, 71 up-to-date); **54 unit, 0 failures/errors/skipped**; Lint **0 помилок / 19 попереджень**; обидва APK зібрано. Source-code та вихідні результати unit/Lint звірено; інструментальна регресія 46 тестів попереднього етапу не повторювалася на телефоні. Замість переобліку старих результатів указано лише 4 свіжі релевантні сценарії.

Фактично доведено: справжній onMapLoaded; 6 маркерів на оглядовій мапі; натискання native маркера → selected ID і camera target; pan → нові дані без скидання камери → повторний ID з відновленням target; інтегровані SiteBrowser/EnergyMap (стабільні тестові readings) marker → видима/виділена office-1 Card, «На мапі», Офіс + Активний + 30–50 кВт → одна Card/мапа, reset → 6; текст popup без ellipsis. Browser test використовує production-компоненти з локальним тестовим станом, не NavHost/MainActivity та не реальну телеметрію.

Чотири оригінальні GoogleMap.snapshot з телефона, manifest із UTC-часом/моделлю/API/SHA-256 та інструкція відтворення — [evidence/maps-phone](evidence/maps-phone/README.md). Це SDK bitmap, не screenshot решти Compose-екрана. Переглянуто контактний аркуш, фінальні 02/04 окремо: назва, тип/стан, опис і показники повністю видимі, в короткому слоті заголовок більше не обрізається. Google attribution збережено. Початкові UiAutomation-кадри були замінені SDK snapshots після перевірки: UiAutomation міг захопити попередній native render frame відразу після camera update. Assertions камери й кінцеві snapshots не підміняють відео всієї послідовності.

Відкриті сценарії: фізичний GPS/зелений маркер, системні coarse/fine/відмови/відкликання, реальний network provider, неправильний ключ/обрив мережі/повтор мапи, усі критерії сортування на справжньому SDK, остаточний звіт і подання. Первинний TLS-збій AVD нижче є історією середовища; він більше не блокує позитивний Maps-сценарій на телефоні. Повного завершення всіх системних сценаріїв не заявлено.

## Повторна перевірка Google Maps — 16.11.2026

Користувач додав власний MAPS_API_KEY до локального виключеного local.properties. Значення не виводилося й не додавалося до Git. Наявність ключа не є підтвердженням його хмарної авторизації.

Перша збірка не пройшла через відсутній jlink у JRE VS Code. Повтор із повним JDK Android Studio успішний (53 с). Без зміни файлів проєкту використано параметри:

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain `
  '-Porg.gradle.java.installations.paths=C:/Program Files/Android/Android Studio/jbr' `
  '-Dorg.gradle.java.home=C:/Program Files/Android/Android Studio/jbr' --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain `
  '-Pandroid.testInstrumentationRunnerArguments.class=ua.kpi.energyforecast.pw04.GoogleMapSdkTest' `
  '-Porg.gradle.java.installations.paths=C:/Program Files/Android/Android Studio/jbr' `
  '-Dorg.gradle.java.home=C:/Program Files/Android/Android Studio/jbr' --no-daemon
```

Фактично запущено **2 тести: 2 failures, 0 errors, 0 skipped**; обидва завершились ComposeTimeoutException після 45000 мс очікування справжнього onMapLoaded. Gradle BUILD FAILED за 2 хв 18 с. Сирий XML збережено у виключеному build/verification/maps-first-sdk.xml. Це вже не відсутній ключ/JUnit assumption, але SDK-перевірки досі не пройдені.

Діагностика Pixel_6 AVD / API 36: boot_completed=1, airplane_mode=0, мережа VALIDATED; DNS maps.googleapis.com та ping успішні. Android-сервіси масово повідомляють SSLHandshakeException / CertPathValidatorException: Trust anchor for certification path not found, у тому числі для android.googleapis.com/checkin. В обмеженій перевірці логів немає Authorization failure; це не доводить коректність ключа, оскільки TLS може блокувати звернення до перевірки авторизації. Ймовірна локальна HTTPS-перевірка/ланцюжок сертифікатів; конкретну причину перехоплення цим запуском не доведено.

Фактичний знімок екрана переглянуто: порожній компонент Google Maps із логотипом/zoom-controls та повідомленням timeout, без тайлів/маркерів. Він залишений лише в build/verification/maps-current.png для діагностики, не поданий як успішний доказ Maps. Перевірку TLS або антивірус не вимикали; production-код не змінено.

Наступний крок: повторити два SDK-тести на Android-пристрої/AVD, де HTTPS Google працює з довіреним ланцюжком. Після цього перевірити авторизацію ключа, InfoWindow, двосторонню синхронізацію, фільтри й зібрати справжні кадри. Старі записи про відсутній ключ нижче є історією попередніх етапів.

## Початкова перевірка реалізації до аудиту етапу 1

| Перевірка | Результат |
|---|---|
| `:app:testDebugUnitTest` | 19 тестів: 9 SiteQueryTest, 8 TelemetryEngineTest, 2 ForecastViewModelTest; 0 помилок, 0 пропусків |
| `:app:connectedDebugAndroidTest` | 4 тести, 0 помилок, 0 пропусків; Pixel_6 AVD, Android 16 / API 36, emulator-5554 |
| `:app:assembleDebug` | Успішно; `app/build/outputs/apk/debug/app-debug.apk` |
| `:app:lintDebug` | Успішно; 0 помилок, 22 попередження: 15 про доступні нові версії AGP/залежностей, 7 UseTomlInstead |
| Збереження інших проєктів | `git diff --exit-code -- PW01 PW02 PW03 CW` — без змін |

Локальні тести перевіряють формулу та одиниці, валідацію, одночасні фільтри, включні межі, порядок за кількома критеріями, незалежні оновлення моделі, холодний старт, обмежені повтори з backoff, останній кадр під час збою, FAILED, скасування, спільний producer, відписку через 5 с, перезапуск і збереження критеріїв.

Завершальний запуск після уточнення backup manifest: `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --offline --console=plain` — `BUILD SUCCESSFUL`, 86 задач (14 виконано, 72 up-to-date). Локальні тести в цьому повторі up-to-date від попереднього успішного виконання незміненого Kotlin-коду; 4 інструментальні тести виконано повторно. Усі залежності вже були в кеші, тому параметри тимчасового TLS-сховища для цього запуску не знадобилися. Створений для тестів headless AVD після перевірок закрито.

Три SiteBrowserTest перевіряють прокрутку/виділення після події **тестового** маркера, вибір картки/деталі та пошук/порожній результат/скидання. Заміна мапи кнопкою не перевіряє SDK Google Maps. AppSmokeTest запускає справжню MainActivity з типовою ViewModel factory, чекає симуляційні дані й перевіряє перехід каталог → деталі → назад.

Автоматичні тести фактично запущено на AVD, це не ручна перевірка мапи, GPS чи візуального оформлення. API-ключ не наданий і не створювався. Фізичний пристрій не використовувався.

Артефакти поточного запуску (ігноруються Git):

- `app/build/test-results/testDebugUnitTest/TEST-*.xml` та `app/build/reports/tests/testDebugUnitTest/index.html`.
- `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_6(AVD) - 16.xml` та `app/build/reports/androidTests/connected/debug/index.html`.
- `app/build/reports/lint-results-debug.xml` та `.html`.
- `app/build/outputs/apk/debug/app-debug.apk` і `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

## Особливості середовища та виправлення

Початковий Gradle-запуск без підвищення доступу не міг створити lock у кеші користувача. Після дозволеного запуску поза sandbox завантаження Maps Compose блокувала помилка PKIX: JDK не довіряв сертифікату локального Avast TLS-scanning. Спроба використати Windows-ROOT безпосередньо в цьому JBR не спрацювала.

Для перевірок створено лише локальну копію штатного JDK cacerts у `build/verification/cacerts` і додано до неї сертифікат Avast із уже довіреного Windows Root. TLS-перевірку не вимикали, JDK та системне сховище не змінювали. Параметри успішного запуску з теки PW04:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --console=plain `
  '-Djavax.net.ssl.trustStore=C:/Projects/RPZMP/PW04/build/verification/cacerts' `
  '-Djavax.net.ssl.trustStorePassword=changeit' `
  '-Djavax.net.ssl.trustStoreType=JKS'
```

Це локальна особливість машини, не обов'язкові параметри проєкту. `changeit` — стандартний пароль цілісності копії публічних сертифікатів. Копія, сертифікат і логи не входять до репозиторію. На іншій машині слід використовувати штатне довірене сховище свого середовища.

Під час розширення тестів знайдено помилку teardown тесту: Main dispatcher відновлювався до завершення скасування дочірніх корутин. Виправлено очікування завершення через runCurrent до resetMain, після чого всі 19 тестів пройшли. Успішний Gradle-запуск містив попередження protobuf/sun.misc.Unsafe із тестового інструментарію; воно не є падінням тесту.

## Перевірка після аудиту етапу 1

База: `032c5884d365c283343df0d46676eb57f7be90de`, локальні зміни аудиту; дата 11.11.2026. Повна матриця та відкриті питання — [requirements.md](requirements.md) і [audit-stage-1.md](audit-stage-1.md).

Виправлено пряму розбіжність із с. 25: натискання Card тепер відкриває деталі. Окрема дія «На мапі» обирає ID й не виконує навігацію. Показано окремо дату оновлення опису та час виміру.

Фактично виконано з теки PW04:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain
```

| Перевірка цього етапу | Результат |
|---|---|
| Локальні тести | Повторно виконано 19: 2 ViewModel, 9 SiteQuery, 8 TelemetryEngine; failures/errors/skipped = 0 |
| Інструментальні тести | Виконано 5: 4 SiteBrowserTest і 1 AppSmokeTest; failures/errors/skipped = 0; Pixel_6 AVD, Android 16 / API 36 |
| Регресія Card | `cardClickOpensDetailsForTheSelectedId` перевіряє callback Card та кнопки «Деталі» |
| Регресія вибору без переходу | `mapActionSelectsCardWithoutOpeningDetails` перевіряє selected semantics і відсутність callback деталей |
| Справжня навігація Activity | Smoke-тест: дані → каталог → «На мапі» → вибрана Card → екран деталей → назад |
| Lint | 0 помилок, 22 попередження (15 про версії, 7 UseTomlInstead) |
| APK застосунку й тестів | `assembleDebug` та `assembleDebugAndroidTest` успішні |
| Gradle | Перший запуск: BUILD SUCCESSFUL, 85 задач (20 виконано); connected-запуск: BUILD SUCCESSFUL, 72 задачі (1 виконано) |

Підтвердження прочитано з XML у `app/build/test-results/testDebugUnitTest/`, `app/build/outputs/androidTest-results/connected/debug/` та `app/build/reports/lint-results-debug.xml`. Поточні звіти у build відповідають цьому запуску й замінюють попередній UI-звіт із 4 тестами. Створений для аудиту headless AVD закрито після перевірки.

Додатково: `git diff --check` без помилок; посилання між Markdown-файлами PW04 перевірено на наявність цільових файлів; `git diff --exit-code -- PW01 PW02 PW03 CW` без змін. Комітів і push для аудиту не виконано. Реальні Google Maps, GPS, ручний pan, відмови дозволів і ручну візуальну перевірку цього разу не виконували. Знахідки A02–A04 встановлено статичним аналізом; їх виправлення не заявляється.

## Перевірка після етапу 2 — A02/A03

Дата 11.11.2026, гілка `feature/pw04`, база `032c588`, збережені незакомічені зміни етапу 1. Реалізація й політика: [stage-2-flow.md](stage-2-flow.md). A02/A03 закрито кодом та автоматичними перевірками. Повторний фокус мапи A04, SDK/GPS A05/A06 і звіт A07 залишаються відкритими.

Фактично виконані команди з теки PW04:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --offline --console=plain
```

| Перевірка поточного коду | Підтверджений результат |
|---|---|
| Unit | 37: TelemetryEngineTest 21, DemoTelemetrySourceTest 3, ForecastViewModelTest 4, SiteQueryTest 9; failures/errors/skipped = 0 |
| UI на пристрої | 9: AppSmokeTest 2, SiteBrowserTest 4, StreamStatusTest 3; failures/errors/skipped = 0; Pixel_6 AVD, Android 16 / API 36 |
| Демонстрація відновлення | `bothDemoSourcesShowFailureAndRecoverInRealActivity` у справжній MainActivity натискає обидві кнопки збою й чекає RETRYING/застарілість → LIVE незалежно для лічильника та моделі |
| Відсутні дані в UI | Немає підміни NO_DATA порожнім пошуком; є restart; до першого результату немає виміряного нуля |
| Скасування та життєвий цикл | Virtual-time: таймер, producer і друга підписка скасовуються; backoff не запускає нову спробу після cancellation; після 5 с без підписників обидва джерела зупиняються; після повторної підписки немає старого LIVE |
| Lint | 0 помилок, 22 попередження: 15 про версії AGP/залежностей, 7 UseTomlInstead |
| Збірка | Debug APK і androidTest APK зібрано; фінальний Gradle-запуск BUILD SUCCESSFUL, 86 задач: 3 виконано, 83 up-to-date |

Unit-тести поточного коду фактично виконано в попередньому запуску цієї серії; у фінальному повторі вони up-to-date. Фінальний connected-запуск повторно виконав усі 9 UI-тестів. Підсумки прочитано з XML у `app/build/test-results/testDebugUnitTest/`, `app/build/outputs/androidTest-results/connected/debug/` та `app/build/reports/lint-results-debug.xml`. Ці артефакти замінили звіти етапу 1.

Перший connected-запуск застав AVD ще offline й завершився `No online devices found`; це не помилка тесту застосунку. Після перевірки ADB `device` та `sys.boot_completed=1` запуск повторено успішно. Збережено пояснення, щоб невдалу спробу не підміняти нібито успішним першим запуском. Попередження protobuf/sun.misc.Unsafe походить від тестового інструментарію.

Це автоматична демонстрація на AVD, а не ручне візуальне QA. Час backoff і watchdog підтверджено virtual-time unit-тестами, не секундоміром UI. Скріншотів, фізичних лічильників, Maps/GPS або фізичного пристрою на цьому етапі не використовували. Два знімки динаміки та знімки станів для фінального звіту ще належить зібрати. AVD, запущений для цього етапу, після тестів закрито.

## Перевірка після етапу 3 — каталог і деталі

Дата 11.11.2026; гілка `feature/pw04`, база `032c588`; попередні локальні зміни збережено. Опис реалізації — [stage-3-catalog.md](stage-3-catalog.md).

Виконано з теки PW04:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain
```

| Перевірка поточного коду | Результат |
|---|---|
| Unit | 38: DemoTelemetrySourceTest 3, ForecastViewModelTest 5, SiteQueryTest 9, TelemetryEngineTest 21; failures/errors/skipped = 0 |
| UI на Pixel_6 AVD, Android 16 / API 36 | 20: AppSmokeTest 3, SiteBrowserTest 8, SiteDetailsTest 5, DetailsNavigationTest 1, StreamStatusTest 3; failures/errors/skipped = 0 |
| Card | Перевірено назву, тип/стан, опис, три підписані числові показники, дату опису й час виміру; дії Card/«Деталі»/«На мапі» |
| Деталі | Потрібний ID із повної колекції, усі поля з прокруткою; окремі WAITING, FAILED/restart, LIVE/not found; збережений об'єкт позначений застарілим |
| Навігація | Реальний NavHost: Card → деталі → назад; невідомий ID → not found → каталог |
| Відновлення | Реальна MainActivity: запит «Офіс» → office-1 → деталі → ActivityScenario.recreate → той самий маршрут/об'єкт → назад із запитом і selected ID; unit-перевірка нового handle з копією значень |
| Розміщення | StateRestorationTester зберігає приховану мапу та вибір; у синтетичному viewport 640×300 dp остання Card і слот мапи досяжні |
| Lint | 0 помилок, 22 попередження (15 про версії, 7 UseTomlInstead) |
| Збірка | assembleDebug та assembleDebugAndroidTest успішні; APK у стандартних outputs/apk |

Unit-тести 38 фактично виконано після додавання нового тесту SavedStateHandle. Збірки й Lint успішні; connected-запуск виконав 20 тестів і завершився BUILD SUCCESSFUL за 1 хв 23 с (72 задачі: 1 виконано, 71 up-to-date). Підсумки звірено з XML unit, connected/debug та lint-results-debug.xml; це не перенесені результати етапу 2.

Пересоздання Activity не є перевіркою повного process death; синтетичний viewport не є ручною перевіркою всіх орієнтацій або великого шрифту. Google Maps у тестах списку замінено тестовим вмістом, SDK/ключ/GPS не перевірялися. Скріншоти й ручне візуальне QA не виконувалися. Прогноз і механізми retry етапу 2 не змінено, їхні тести збережено. AVD, запущений для цих тестів, після перевірки закрито. Зміни обмежено PW04, commit/push не виконано.

## Перевірка після етапу 4 — пошук, фільтри, сортування

Дата 11.11.2026; гілка `feature/pw04`, база `032c588`; зміни етапів 1–3 збережено. Поведінка та обсяг — [stage-4-query.md](stage-4-query.md).

Виконані команди з теки PW04:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --offline --console=plain
```

| Перевірка поточного коду | Результат |
|---|---|
| Unit | 50: DemoTelemetrySourceTest 3, ForecastViewModelTest 8, SiteQueryAdvancedTest 9, SiteQueryTest 9, TelemetryEngineTest 21; failures/errors/skipped = 0 |
| Інструментальні на Pixel_6 AVD, Android 16 / API 36 | 26: AndroidCollationTest 1, AppSmokeTest 4, DetailsNavigationTest 1, FilterPanelTest 4, SiteBrowserTest 8, SiteDetailsTest 5, StreamStatusTest 3; failures/errors/skipped = 0 |
| Критерії/напрямки | Очікувані ID для всіх 6 перших критеріїв у двох напрямках; кожен другий критерій розв'язує рівність; повні збіги стабільні за ID. Перші критерії/напрямки також фактично вибрані через UI-меню та checkbox |
| Комбінації | 240 перевірок AND у межах одного JUnit-тесту: 4 типи (включно «усі») × 3 статуси × 4 запити × 5 діапазонів; це не 240 окремих тестів |
| Unicode | Регістрові/канонічні форми в unit; правильний український порядок Ґ/Є/І/Ї окремо перевірено у JVM та Android runtime |
| Числа | Кома/крапка/експонента, однобічні/включні/рівні межі; відхилення NaN/Infinity/hex/суфіксів/переповнення/ненульового underflow/перевернутого діапазону |
| UI панелі | Офіс + Активний + 30,0–50 кВт + пошук → office-1; 50–10 → помилка; обидві дії скидання очищають весь SiteQuery |
| MainActivity | Реальна панель керує ViewModel; тип/статус/межі/дата/напрямок і результат збереглися після ActivityScenario.recreate; скидання повернуло 6 об'єктів |
| Вибір | Видимий ID зберігається при сортуванні/оновленні прогнозу; прихований фільтром або помилкою очищається і не воскресає після скидання |
| Lint | 0 помилок, 22 попередження: 15 про версії, 7 UseTomlInstead |
| Збірка | Debug APK і androidTest APK успішні; фінальний Gradle-запуск BUILD SUCCESSFUL за 2 хв 22 с, 86 задач (8 виконано, 78 up-to-date) |

Усі 50 unit-тестів фактично виконано в цій серії після додавання нових тестів; у фінальному повторі вони up-to-date. У фінальному connected-запуску виконано всі 26 інструментальних тестів. Числа звірено з XML у `app/build/test-results/testDebugUnitTest/`, `app/build/outputs/androidTest-results/connected/debug/` та `app/build/reports/lint-results-debug.xml`. Невдалих тестів цієї серії не було. Перед connected-запуском ADB показував device та `sys.boot_completed=1`.

Тестовий слот мапи показував ID visible для перевірки результату, а не справжні маркери Google Maps. Maps SDK/ключ/GPS та ручне візуальне QA не перевірялися; знімків не створювали. Відновлення Activity не є process-death тестом. Автоматичні XML-звіти є доказами виконаних сценаріїв; знімки панелі й результатів для звіту ще потрібні. Створений для етапу 4 headless AVD після тестів закрито. Зміни лише в PW04, без commit/push.

## Етап 5 — програмна частина та відсутній ключ

Гілка `feature/pw04`, база `032c588`; зміни попередніх етапів збережено. Повний опис, передумови та критерії завершення — [stage-5-map.md](stage-5-map.md). API-ключ на початку й наприкінці локальної роботи відсутній; значення секретів не виводили. `git check-ignore` підтвердив виключення local.properties та build-артефактів.

Фактично виконано з теки PW04:

```powershell
.\gradlew.bat :app:signingReport --offline --console=plain
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --offline --console=plain
# Окремий доступний набір без ключа:
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain `
  '-Pandroid.testInstrumentationRunnerArguments.notClass=ua.kpi.energyforecast.pw04.GoogleMapSdkTest'
```

| Перевірка | Фактичний результат / межі |
|---|---|
| Unit | 54: DemoTelemetrySourceTest 3, ForecastViewModelTest 9, MapPresentationTest 3, SiteQueryAdvancedTest 9, SiteQueryTest 9, TelemetryEngineTest 21; failures/errors/skipped = 0 |
| Перша connected-спроба | 33 зареєстровані тести: 31 без failure, 2 failure з `org.junit.AssumptionViolatedException: Потрібен локальний MAPS_API_KEY та перебудова APK`; XML/HTML skipped = 0, Gradle BUILD SUCCESSFUL, test-result-exit-code = 0 |
| Причина двох записів | Обидва — GoogleMapSdkTest, які зупинились на requireMaps ДО створення SDK-мапи. Це не доказ SDK і не падіння перевірених без ключа сценаріїв. Невідповідність XML/Gradle не приховується |
| Доступний набір | GoogleMapSdkTest явно виключено параметром notClass; 31 тест: AndroidCollationTest 1, AppSmokeTest 4, DetailsNavigationTest 1, FilterPanelTest 4, MapConfigurationTest 1, MapFocusTest 3, SiteBrowserTest 9, SiteDetailsTest 5, StreamStatusTest 3 |
| Фокус A04 | ViewModel revision та SiteFocusEffect: повторний ID створює подію, вимір/сортування/callback — ні; очікування готовності/останній видимий вибір; повторна подія повертає LazyColumn до вже вибраної картки |
| Маркери | Дані ID/координат/title/snippet/selected звірені з visible; приховані об'єкти не лишають маркерів; новий прогноз/сортування не переміщують координати |
| Без ключа | Справжній SiteMapPanel показує налаштування і не створює GoogleMap. Реальні тайли/камера/маркери/InfoWindow НЕ перевірені |
| Lint і збірки | 0 помилок, 22 попередження (15 про версії, 7 UseTomlInstead); debug APK та androidTest APK зібрано |
| signingReport | Успішний; публічний SHA-1 debug-підпису наведено у stage-5-map.md. Це не API-ключ і не приватний ключ |

Першу XML/HTML-спробу до перезапису збережено в ігнорованих `build/verification/stage5-sdk-prerequisites.xml` та `stage5-sdk-prerequisites-summary.html`; це сирі звіти, failure-записи не редагувалися. Фінальний доступний набір має звичайні outputs/reports: XML підтверджує 31 tests, 0 failures, 0 errors, 0 skipped. 54 unit-тести фактично виконано в цій серії; наступні повтори були up-to-date. Не можна звітувати «33 SDK/UI успішні» або називати два failure-записи skipped у сирому XML.

Інструментальні перевірки виконувались на Pixel_6 AVD, Android 16 / API 36, після підтвердження ADB device та boot_completed=1. Реальні GoogleMapSdkTest підготовлено й скомпільовано, але потребують налаштованого ключа та нового запуску без notClass. Завантажувальний timeout 20 с/повтор мапи реалізовано в SDK-гілці; його фактичний сценарій з неправильним ключем/мережею ще не перевірено.

Жодних знімків справжньої мапи, ручного візуального QA або GPS-результатів не створено/не заявлено. A04 програмно виправлено, A05 та SDK-доказ A04 очікують ключа; A06/A07 лишаються наступними етапами. Створений AVD після доступних перевірок закрито. Зміни тільки PW04, без commit/push; інші проєкти й вкладення збережено.

## Етап 6 — перевірка 13.11.2026

Гілка `feature/pw04`, база `032c588`; попередні локальні зміни збережено. Maps за вказівкою користувача відкладено. Реалізація й межі — [stage-6-location.md](stage-6-location.md).

Фактично виконані команди з PW04 (JAVA_HOME — JBR Android Studio):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest :app:lintDebug --offline --console=plain `
  '-Pandroid.testInstrumentationRunnerArguments.notClass=ua.kpi.energyforecast.pw04.GoogleMapSdkTest'
# Після додавання AndroidLocationTest — окремий інтеграційний запуск:
.\gradlew.bat :app:connectedDebugAndroidTest :app:lintDebug --offline --console=plain `
  '-Pandroid.testInstrumentationRunnerArguments.class=ua.kpi.energyforecast.pw04.AndroidLocationTest'
```

| Перевірка | Фактичний результат |
|---|---|
| Unit | 54, failures/errors/skipped = 0; виконано після змін LocationSource, тестів Android у JVM немає |
| Регресія на Pixel_6 / API 36 | 39, failures/errors/skipped = 0: попередній доступний набір 31 + LocationSourceTest 8; BUILD SUCCESSFUL за 3 хв 30 с |
| Android GPS інтеграція | Окремо 1, failures/errors/skipped = 0; AndroidLocationTest, BUILD SUCCESSFUL за 35 с |
| Унікальні instrumented-сценарії серії | 40 у двох запусках (39 + 1), а не один повний запуск 40 |
| Збірка | Debug APK і androidTest APK успішні; останній APK містить новий AndroidLocationTest |
| Lint фінальний | 0 помилок, 19 попереджень: GradleDependency 8, NewerVersionAvailable 4, UseTomlInstead 7; попередні 22 у старих секціях є історичними результатами |
| Додаткове попередження компілятора | Deprecated Criteria у тестовому addTestProvider; не помилка збірки й не production-API зміна |
| Git | diff --check успішний; PW01–PW03/CW без змін; staging порожній, commit/push не виконано |

XML першого запуску збережено до перезапису в ігнорованому `build/verification/stage6-regression-39.xml`; другого — `stage6-android-gps.xml`. Сирі XML обох запусків містять нуль failures/errors/skipped. Підсумки unit і Lint звірено з XML. До першого connected-запуску ADB підтвердив boot_completed=1. AVD створено для перевірок і закрито після них.

LocationSourceTest використовує тестовий platform adapter: доведено coarse/network policy, реєстрацію вимкнених джерел, callback-відновлення, два виміри, відсутній network, відкидання невалідних координат/accuracy, відкликання, SecurityException та зняття підписки на STOP/DESTROY із новою підпискою на START. LifecycleRegistry не є ручним згортанням реальної Activity.

AndroidLocationTest використовував справжній LocationSource(Context), AndroidLocationPlatform і LocationManager із системним test provider GPS: foreground Activity, runtime grants через UiAutomation, позиції 50.4501/30.5234 та 50.4601/30.5234, точність 5 м, очікування 5.5 с між вимірами та очищення позиції після вимкнення GPS-провайдера. Це синтетичні дані Android test provider на AVD, не фізичний GPS і не `emu geo fix`. Test provider видалений у finally, mock-location app-op скинутий до default. Після завершення Gradle пакет уже був видалений із AVD: додаткові pm revoke/appops get повернули package not found; це не невдала перевірка GPS і не підтвердження зміни дозволів на фізичному пристрої.

Діалоги «точно/приблизно», одноразова/постійна відмова, відкликання через налаштування, реальний network provider та згортання/повернення Activity ще потребують ручного проходження. Зеленого маркера Maps, фізичного GPS, screenshots або ручного візуального QA не перевіряли. Два GoogleMapSdkTest не запускались за домовленістю про відкладений етап 5. Етап 6 має код і автоматичні докази доступних сценаріїв; повний набір системних/візуальних доказів лишається відкритим.

## Етап 7 — адаптивність і регресія, 13.11.2026

Гілка `feature/pw04`, база `032c588`; попередні зміни збережено. Деталі реалізації й межі — [stage-7-regression.md](stage-7-regression.md). API-ключ і справжній Maps за домовленістю відкладено.

Фактично виконані команди з PW04:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --offline --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain `
  '-Pandroid.testInstrumentationRunnerArguments.class=ua.kpi.energyforecast.pw04.ResponsiveUiTest,ua.kpi.energyforecast.pw04.ActivityLifecycleTest'
# Після виправлення панелі:
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:connectedDebugAndroidTest --offline --console=plain `
  '-Pandroid.testInstrumentationRunnerArguments.notClass=ua.kpi.energyforecast.pw04.GoogleMapSdkTest'
```

Початкова збірка успішна за 43 с; 54 unit-тести, failures/errors/skipped = 0. Початковий targeted-запуск фактично виконав лише 4 ResponsiveUiTest (підтверджено XML і logcat), попри перелік двох класів у параметрі; він не зараховується як 6 тестів або як доказ lifecycle. Результат — 1 failure у largeFontFilterSheetCanReachResetAndDone: «Скинути» не видно після прокрутки до «Готово». Gradle BUILD FAILED за 1 хв 3 с. Сирий звіт збережено в ігнорованому `build/verification/stage7-initial-responsive-4.xml`.

Виправлення: дії стали окремими елементами LazyColumn, тест прокручує до кожної. Повторний запуск використовує весь доступний набір через notClass, а не перелік класів. Оскільки цей запуск включає системний GPS test provider, підсумок стосується AVD із Google Play services; це не фізичний GPS.

Перший повний повтор виконав 46 тестів, 1 failure / 0 ignored у TestRunner; Gradle BUILD FAILED за 3 хв 1 с. Та сама дія «Скинути» залишалася обрізаною: розділення кнопок недостатньо. TestRunner-лог цих спроб збережено в `build/verification/stage7-test-runner.log`; XML проміжного повного запуску не був збережений до перезапису, тому він не заявляється як окремий артефакт.

Панель переведено у повністю розгорнутий стан (skipPartiallyExpanded=true). Потім фактично виконано окрему команду з `-Pandroid.testInstrumentationRunnerArguments.class=ua.kpi.energyforecast.pw04.ResponsiveUiTest`: 4 tests / 0 failures / 0 errors / 0 skipped, BUILD SUCCESSFUL за 33 с. XML збережено в `build/verification/stage7-responsive-4-pass.xml`. Це доказ виправлення targeted-сценарію, а не заміна повної регресії.

Фінальний другий повний запуск: **46 tests, 0 failures, 0 errors, 0 skipped**, звірено з XML; BUILD SUCCESSFUL за 2 хв 37 с, 86 задач (11 виконано, 75 up-to-date). Класи: ActivityLifecycleTest 2, AndroidCollationTest 1, AndroidLocationTest 1, AppSmokeTest 4, DetailsNavigationTest 1, FilterPanelTest 4, LocationSourceTest 8, MapConfigurationTest 1, MapFocusTest 3, ResponsiveUiTest 4, SiteBrowserTest 9, SiteDetailsTest 5, StreamStatusTest 3. Сирий XML збережено у `build/verification/stage7-final-46.xml` і стандартному outputs/androidTest-results/connected/debug.

54 unit-тести: failures/errors/skipped = 0; Lint фінального коду — 0 помилок / 19 попереджень. Debug APK та androidTest APK успішно зібрані. Два GoogleMapSdkTest виключені явно та не входять у 46: без ключа вони залишаються відкладеними. ADB перед запуском підтвердив boot_completed=1. Створений Pixel_6 AVD / API 36 закрито після тестів.

ActivityLifecycleTest фактично перевів MainActivity у CREATED/RESUMED, а також змінив орієнтацію landscape → portrait і дочекався відповідного Configuration; пошук «Офіс», office-1 та прихована мапа збереглися. ResponsiveUiTest підтвердив доступність дій при синтетичному fontScale=2; це не зміна системного шрифту всього AVD. Всі 8 тестів platform adapter і системний GPS test provider етапу 6 також пройшли у цьому спільному запуску.

Git diff --check та відносні посилання Markdown перевірено; змін PW01–PW03/CW немає, staging порожній. Попередні локальні зміни та вкладення збережено, commit/push не виконано.

Ручне візуальне QA, скриншоти, process death, TalkBack, foldable/multi-window та справжній Google Maps цими перевірками не підтверджуються. Системні діалоги дозволів/відмови та реальний network provider з етапу 6 також лишаються відкритими.

## Етап 8 — звіт і справжні скриншоти, 13.11.2026

Production-код після успішної регресії етапу 7 не змінено. Додано EvidenceCaptureTest, скрипт збору та документацію. Виконано з PW04:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --offline --console=plain
python scripts/collect_evidence.py
```

Gradle BUILD SUCCESSFUL за 7 с. Скрипт установив APK на створений Pixel_6 AVD / API 36 та виконав `am instrument -w -r -e class ua.kpi.energyforecast.pw04.EvidenceCaptureTest ua.kpi.energyforecast.pw04.test/androidx.test.runner.AndroidJUnitRunner`. Фактичний результат AndroidJUnitRunner: **OK (1 test), Time: 19.635 с**, INSTRUMENTATION_CODE=-1 (звичайне успішне завершення runner). Сирий лог — у виключеному `build/verification/stage8-evidence-instrumentation.txt`. Це один новий окремий сценарій, не повтор повних 46 тестів етапу 7. Unit-тести не повторювалися без зміни production-коду; останній підтверджений результат — 54 успішні. Lint 0 помилок / 19 попереджень.

Отримано 7 оригінальних PNG 1080×2400 через UiAutomation.takeScreenshot, скопійовано побайтово через adb exec-out/run-as. Вони й manifest із UTC-часом/моделлю/API/SHA-256 розміщені в [evidence](evidence/README.md). Переглянуто контактний аркуш усіх кадрів та окремий кадр панелі фільтрів. Виявлено й задокументовано, що 05/06 мають однаковий SHA-256: усі деталі вже вміщаються; окремого руху прокрутки ці два PNG не доводять. Контактний аркуш — лише локальний QA-артефакт у build/verification/stage8-contact.png, оригінали в документації не редагувалися.

Звірено 31 унікальний ID матриці (30 номерованих вимог занять + DOC1), 48 контрольних відповідей, усі відносні Markdown-посилання та SHA-256 семи PNG. Python-скрипт перевірено на синтаксис і фактично виконано. Пошук типових шаблонів ключів у видимих Git файлах PW04 збігів не знайшов; секретні значення не виводилися. git diff --check успішний, PW01–PW03/CW без змін, staging порожній. Установлені для доказів тестовий і основний пакети видалені (обидва adb uninstall: Success), створений AVD закрито.

Звіт, README, матриця вимог і стан A07 узгоджені з фактичними результатами. Повторно звірені питання/критерії методички; нових невідповідностей варіанта не виявлено. Повний звіт у PDF/DOCX і подання не створювалися. Відкладені Maps/системні докази перелічені в [stage-8-report.md](stage-8-report.md). Старі записи «скриншотів немає» нижче/в попередніх етапах стосуються тих дат і конкретних сценаріїв; тепер для доступної частини є сім перелічених кадрів, але Maps/GPS/дозволів серед них немає.

## Історичний перелік наступних перевірок до фінального збору

Після налаштування власного ключа Google Maps виконати такі сценарії й записати модель пристрою, Android API, дату та фактичний результат:

1. **Потоки:** два послідовні знімки моніторингу з різним часом виміру; натиснути «Збій лічильника», зробити знімок RETRYING із позначкою застарілості, потім LIVE; повторити для «Збій моделі». Автоматично цей сценарій уже перевірено в етапі 2, але знімки для звіту ще не збиралися.
2. **Мапа:** знімок завантаженої справжньої Google Maps із маркерами всіх видимих споживачів. Натиснути «На мапі» у картці — зафіксувати камеру на її координатах і блакитний маркер. Після ручного pan повторити цю дію для того самого об'єкта (A04 програмно виправлено, SDK-доказ ще потрібен). Натиснути інший маркер — зафіксувати InfoWindow з назвою/типом/описом, виділену картку та прокрутку. Знімки або короткий запис екрана повинні показувати обидва напрямки. Натискання самої картки має відкривати окремий екран деталей.
3. **Комбіновані фільтри:** встановити тип «Офіс», статус «Активний», потужність 30–50 кВт; має залишитися лише «Офіс Обрій» і один справжній маркер. Зняти фільтри, задати «Тип → Встановлена потужність», перевірити напрямки. Окремо перевірити дату опису та статус. Логіку й UI автоматично перевірено в етапі 4; кадри панелі й результату вже отримано на етапі 8; окремі кадри інших критеріїв/напрямків і реальні маркери ще потрібні.
4. **Порожній/помилковий запит:** пошук неіснуючої назви (кадр уже є) та межі 50–10 (кадр помилки ще потрібен); потім скидання. Перевірити, що прихований об'єкт більше не виділений.
5. **GPS:** дозволити точну позицію; на AVD задати координати через Extended Controls або `adb -s emulator-5554 emu geo fix 30.5234 50.4501`. Знімок має містити зелений маркер, статус і точність. Змінити координати й підтвердити оновлення (врахувати 5 с/10 м). Це буде перевіркою імітованого GPS AVD, а не фізичного GPS.
6. **Приблизний дозвіл:** перевірити на пристрої/AVD із доступним мережевим провайдером; знімок приблизної позиції або чесного повідомлення про недоступне джерело, без фіктивного маркера.
7. **Відмови:** відхилити дозвіл, потім відхилити постійно; знімок повідомлення й перевірка «Дозволи». Вимкнути геолокацію — перевірити повідомлення й повернення через «GPS»/«Моя позиція». Відкликати доступ у системних налаштуваннях — повернення без аварії.
8. **Життєвий цикл:** повернути пристрій із заданими фільтрами й вибором; перевірити відновлення. Вийти з екрана мапи/згорнути застосунок і перевірити припинення location updates; через 5 с без UI-підписок зупиняються генератори. Повернутися — переконатися у відновленні збору.
9. **Ключ/мережа/сервіси:** відсутній ключ (інструкція замість мапи), неправильний/обмежений ключ, відсутня мережа, недоступні Play services. Каталог і прогноз мають лишатися придатними для роботи; зберегти справжні повідомлення/логи Maps без ключа в кадрі.
10. **Вигляд:** перевірити портретну/альбомну орієнтацію та збільшений шрифт; деталі й панель фільтрів мають прокручуватися. Знімок деталей із кВт, кВт·год, режимом і координатами вже отримано на етапі 8; кадри орієнтацій/великого системного шрифту ще потрібні.

Наявні кадри етапу 8 показують LIVE-моніторинг, каталог без ключа/дозволу, панель комбінованих фільтрів і результат office-1, деталі та нуль збігів пошуку — див. evidence/README.md. Для сценаріїв вище ще потрібні Maps/GPS/системні дозволи, retry, помилка діапазону, інші сортування й кадри орієнтації/великого системного шрифту. Порожніх файлів або вигаданих зображень немає. Звіт потребує цих доказів і остаточного потрібного викладачу оформлення. Commit/push і подання не виконано; публікація до GitHub є окремим наступним кроком.

Підсумковий контроль телефонної серії: усі відносні посилання перевірено; SHA-256 чотирьох Maps PNG звірено, API-ключ не знайдено у видимих Git файлах PW04. `git diff --check -- PW04` успішний, staging порожній, гілка feature/pw04. Хеш diff інших проєктів відрізняється від раніше знятого snapshot: не заявляється побайтова незмінність чужої паралельної роботи. Команди цього проходу редагували лише PW04; жодних змін інших проєктів не скасовували й не включали в staging. Тестовий пакет PW04 видалено з телефона (Success), основний застосунок залишено встановленим та відкрито MainActivity для користувача. Commit/push не виконано.
