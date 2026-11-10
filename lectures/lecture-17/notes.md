# Лекція 17 — Async/await, диспетчери, скасування та канали

## Основне

### Async, Deferred та результат

```kotlin
val first: Deferred<Int> = async { sum(1, 2) }
val second = async { sum(3, 4) }
println(first.await() + second.await())
```

- Async створює дочірню корутину з результатом типу T і повертає `Deferred<T>`. Deferred також є Job: має start, cancel, join і властивості стану.
- Await призупиняє поточну корутину до готовності результату; повертає T або кидає виняток обчислення/скасування.
- Join чекає завершення без повернення значення. Await отримує саме значення.
- Щоб незалежні обчислення просувалися конкурентно, спочатку створіть усі async, а потім чекайте результати.
- `async { a() }.await(); async { b() }.await()` зазвичай виконує ці роботи послідовно: другу корутину ще не створено, поки першу не дочекалися.
- Async не є способом ізолювати помилки дитини. Звичайний збій дитини скасовує батьківський scope, навіть якщо await ще не викликано. Для іншої політики потрібні окремі засоби нагляду.
- Для `CoroutineStart.LAZY` запуск відбувається через start, await або join. Послідовне await двох lazy-об'єктів може послідовно запустити роботи; якщо потрібна конкурентність, спочатку викличте start для обох.

### Диспетчер і контекст

| Засіб | Призначення |
| --- | --- |
| Успадкований контекст | Launch/async зазвичай беруть диспетчер батьківської області |
| `Dispatchers.Default` | Пул для CPU-обчислень |
| `Dispatchers.IO` | Блокувальні операції введення/виведення на JVM |
| `Dispatchers.Main` | Головний UI-потік за наявності відповідного модуля |
| `Dispatchers.Unconfined` | Спеціальний режим без фіксованого диспетчера після призупинення |
| `withContext(dispatcher)` | Виконати блок в іншому контексті й дочекатися результату |
| `newSingleThreadContext(...)` | Створити власний виділений потік; ресурс треба закрити |

- Default й IO можуть використовувати спільні потоки; перемикання контексту не завжди означає інший фактичний потік.
- IO не робить операцію неблокувальною: він дозволяє винести блокувальний виклик із UI-потоку. Уже неблокувальний suspend-API може сам керувати потрібним контекстом.
- Main не доступний у звичайному консольному JVM-проєкті лише з core. Для Android його забезпечує відповідний модуль; приклади не запускають Dispatchers.Main.
- Unconfined починає в поточному потоці до першого фактичного призупинення; подальший потік залежить від suspend-операції. Це не звичайний вибір для UI чи фонової роботи.
- У навчальному прикладі `newSingleThreadContext("LectureWorker").use { ... }` закриває власний потік після withContext. Не створюйте новий виділений потік на кожне звичайне завдання.
- Для API, позначених Experimental/Delicate, файл містить вузькі `@OptIn`. Це явне прийняття особливостей конкретних API, а не глобальне приховування попереджень.

### Скасування

- `job.cancel()` запитує скасування; не чекає завершення очищення. `job.cancelAndJoin()` запитує скасування й чекає завершення Job.
- Скасування **кооперативне**: код має дійти до перевірки стану або cancellable suspend-виклику, наприклад delay.
- CPU-цикл без suspend-викликів може не реагувати на cancel. Додавайте `ensureActive()`, перевірку isActive чи доречний yield.
- CancellationException — сигнал керування життєвим циклом, а не звичайна помилка завантаження.

```kotlin
try {
    awaitCancellation()
} catch (e: CancellationException) {
    println("Скасовано")
    throw e
} finally {
    println("Звільняємо ресурси")
}
```

- Усередині скасованої роботи, якщо перехопили CancellationException для логування, зазвичай передайте його далі через throw. Не поглинайте його широким catch(Exception).
- Finally виконує звичайне очищення при скасуванні. Suspend-очищення може вимагати спеціального NonCancellable-контексту; не використовуйте його для продовження непотрібної бізнес-роботи.
- Await уже скасованого Deferred кидає CancellationException. У прикладі його обробляє активний батько, який сам скасував цю дитину; ensureActive перевіряє, що самого батька не скасовано.
- `CompletableDeferred<Unit>` у прикладі — одноразовий сигнал «дитина почала роботу». Батько await цього сигналу, потім cancelAndJoin. Це усуває залежність демонстрації від того, хто встиг за задану кількість мілісекунд.
- AwaitCancellation призупиняється до скасування; не є імітацією реального завантаження.

### Channel та producer-consumer

```kotlin
val channel = Channel<Int>()
launch {
    try {
        for (number in 1..3) channel.send(number)
    } finally {
        channel.close()
    }
}
for (number in channel) println(number)
```

- Channel передає значення між корутинами: `send(value)` надсилає, `receive()` отримує. Обидві функції можуть призупинятися.
- Звичайний `Channel<T>()` — rendezvous-канал без буфера: передавання узгоджується з отримувачем.
- Буфер дозволяє зберігати певну кількість значень; після заповнення send може чекати. Receive чекає, якщо ще немає значення й канал не закритий.
- `close()` означає, що нові значення більше не надсилатимуться. Уже надіслані/буферизовані значення можна дочитати; for завершується після їх отримання при нормальному закритті.
- Close і cancel каналу не тотожні: cancel припиняє канал із відкиданням залишку; нормальне close дозволяє дочитати.
- Якщо кількість значень відома, можна використовувати repeat(n) із receive. Якщо надіслано менше, зайвий receive може чекати або отримати помилку закриття; for + close часто зручніші.
- `fun CoroutineScope.produceUsers(): ReceiveChannel<String> = produce { ... }` запускає producer-корутину та повертає інтерфейс для читання каналу. Producer закриває канал після свого завершення.
- `consumeEach { ... }` споживає канал і завершує його споживання, зокрема скасовує канал при достроковому виході/помилці.
- Канал не є широкомовним журналом: із кількома отримувачами звичайне значення передається одному з них. Повторне читання не відтворює історію заново.

## Приклади з `tasks.kt`

Це **навчальні приклади за змістом PDF**, а не окремо сформульовані викладачем практичні завдання. Окремого переліку завдань чи контрольних запитань у PDF немає.

1. Три async із результатами 3, 7, 11; lazy await; конкурентний start двох lazy-робіт.
2. Успадкований диспетчер, Default, IO, Unconfined та закритий власний потік LectureWorker.
3. Скасування запущеної дитини: сигнал готовності, CancellationException, throw і finally.
4. Await скасованого Deferred; активний батько продовжує роботу.
5. CPU-цикл із ensureActive: кількість ітерацій залежить від планувальника.
6. Send/receive відомої кількості чисел; буферизований канал і for до close.
7. Produce/ReceiveChannel/consumeEach із трьома іменами.

## Питання для самоперевірки

Навчальні питання з короткими відповідями, додані для повторення.

1. Чим launch відрізняється від async? Launch повертає Job без значення роботи; async — Deferred<T> з результатом.
2. Чи await блокує потік? Ні, може призупинити корутину.
3. Чому два lazy await можуть дати послідовне виконання? Друге обчислення ще не запущене до другого await.
4. Для чого IO? Для блокувальних операцій введення/виведення, які треба винести з чутливого потоку.
5. Чому cancel не завжди миттєво зупиняє CPU-цикл? Скасування потребує співпраці коду.
6. Що робити з CancellationException після логування в самій роботі? Зазвичай кинути далі.
7. Навіщо cancelAndJoin? Щоб продовжити після фактичного завершення скасованої роботи й її очищення.
8. Чому for по Channel може не завершитися? Якщо producer не закрив канал і даних більше немає.
9. Чи close видаляє всі буферизовані значення? Ні, нормальне close дозволяє їх дочитати.
10. Чи produce потребує ручного close після нормального завершення? Ні, закриває канал сам.


## Підключення бібліотеки та запуск із папки lecture-17

Приклади перевірено з **Kotlin/JVM 2.3.21, Java та kotlinx.coroutines 1.10.2**. Версію бібліотеки закріплено для відтворюваності. На відміну від попередніх лекцій, лише стандартної бібліотеки Kotlin недостатньо.

### 1. Один раз завантажте залежність для всіх трьох лекцій

CMD:

```cmd
if not exist "C:\Tools\kotlin-libs" mkdir "C:\Tools\kotlin-libs"
curl.exe -L --fail "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.2/kotlinx-coroutines-core-jvm-1.10.2.jar" -o "C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar"
```

PowerShell:

```powershell
New-Item -ItemType Directory -Force "C:\Tools\kotlin-libs" | Out-Null
Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.2/kotlinx-coroutines-core-jvm-1.10.2.jar" -OutFile "C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar"
```

Якщо файл уже завантажено для лекції 16, повторювати завантаження для 17–18 не потрібно. JAR зберігається поза репозиторієм.

### 2. Скомпілюйте й запустіть поточну лекцію

CMD:

```cmd
cd C:\Projects\RPZMP\lectures\lecture-17
"C:\Tools\kotlinc\kotlinc\bin\kotlinc.bat" tasks.kt -cp "C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar" -include-runtime -d tasks.jar
java -cp "tasks.jar;C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar" TasksKt
```

PowerShell:

```powershell
cd C:\Projects\RPZMP\lectures\lecture-17
& "C:\Tools\kotlinc\kotlinc\bin\kotlinc.bat" tasks.kt -cp "C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar" -include-runtime -d tasks.jar
java -cp "tasks.jar;C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar" TasksKt
```

Замініть шлях до kotlinc, якщо він встановлений в іншій папці. Компілюйте лекції **окремо**: кожна має власний main і може повторювати імена класів/функцій.

- `-cp` при компіляції дає компілятору бібліотеку корутин; при запуску вона також має бути в classpath.
- `-include-runtime` додає Kotlin runtime у tasks.jar, але **не пакує** зовнішню бібліотеку корутин усередину.
- Тому тут потрібен `java -cp ... TasksKt`, а не старий `java -jar tasks.jar`. На Windows роздільник classpath — крапка з комою `;`; TasksKt — JVM-клас із main, згенерований із tasks.kt.
- Якщо бачите `unresolved reference: kotlinx`, перевірте завантажений JAR і параметр -cp. Якщо `NoClassDefFoundError: kotlinx/coroutines/...` — перевірте classpath саме команди java.
- Не додавайте згенерований tasks.jar у Git. Після редагування збережіть tasks.kt і повторіть компіляцію.

### Альтернатива для наявного Gradle/JVM-проєкту

У його build.gradle.kts можна підключити:

```kotlin
repositories { mavenCentral() }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
```

Це альтернатива ручному classpath, а не окремий готовий Gradle-проєкт у цьому архіві. Не вставляйте цей фрагмент у tasks.kt. Приклади є консольними: вони не змінюють Android-проєкти PW01/PW02/PW03/CW.

## Що запам'ятати та уточнення до PDF

- CoroutineScope успадковує контекст; він не завжди сам призначає Dispatchers.Default. Наш runBlocking дає іншу початкову поведінку, ніж suspend main у PDF.
- Правильні назви: `Dispatchers.Default`, `Thread.currentThread().name`, а не Dispatcher.Default чи Name.
- Не можна вважати всі suspend-функції автоматично cancellable в кожній точці. Потрібні фактичні перевірки або відповідні бібліотечні операції.
- У PDF CancellationException поглинається після логування. Тут у самій скасованій корутині він передається далі.
- NewSingleThreadContext і produce мають спеціальні API-позначки в закріпленій бібліотеці. Додано необхідні OptIn; власний диспетчер закривається через use.
- Рядки виведення незалежних корутин та назви робочих потоків можуть відрізнятися між запусками. Це не означає помилку обчислень.

## Джерела

- «РПЗМП Лк 17.pdf», розділи 8.4–8.7, с. 3–18.
- [Kotlin: Composing suspending functions](https://kotlinlang.org/docs/composing-suspending-functions.html).
- [Kotlin: Coroutine context and dispatchers](https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html).
- [Kotlin: Cancellation](https://kotlinlang.org/docs/coroutines-cancellation-and-timeouts.html).
- [Kotlin: Channel API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/).
- [Kotlin: Produce API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/produce.html).
