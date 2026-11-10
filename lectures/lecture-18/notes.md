# Лекція 18 — Асинхронні потоки Flow

## Основне

### Від одного результату до потоку значень

| Засіб | Що дає | Як отримати дані |
| --- | --- | --- |
| `suspend fun load(): T` | Один результат виклику | Викликати із suspend-контексту |
| `Deferred<T>` | Один результат дочірньої корутини | `await()` |
| `List<T>` | Готовий набір елементів | Перебір/операції колекції |
| `Sequence<T>` | Ліниве синхронне отримання елементів | Перебір/термінальна операція |
| `Flow<T>` | Послідовність значень із підтримкою suspend-операцій | Collect або інша термінальна операція |
| `Channel<T>` | Передавання значень між корутинами | Receive/for; прочитані значення споживаються |

Flow зручний, коли значення можна отримувати поступово й між ними потрібно очікувати: наприклад, рядки з джерела даних чи оновлення. Корутина також може повернути цілий List; потік потрібен, коли корисна поступова передача, а не просто «більше одного значення».

### Flow, emit і collect

```kotlin
fun users(): Flow<String> = flow {
    delay(100L)
    emit("Taras")
    delay(100L)
    emit("Olena")
}

suspend fun showUsers() {
    users().collect { name -> println(name) }
}
```

- `Flow<String>` означає потік рядків; `flow { ... }` описує джерело, `emit(value)` передає значення, collect споживає.
- Функція, яка лише створює Flow, зазвичай не потребує suspend: тіло flow виконається пізніше у suspend-контексті збирача.
- Flow, створений через flow/flowOf та використані тут asFlow, є **холодним (cold)**: побудова об'єкта сама не запускає тіло джерела.
- Термінальна операція запускає збір. Повторний collect запускає холодне джерело заново; автоматичного кешу результатів немає.
- Це правило не стосується всіх реалізацій Flow: StateFlow/SharedFlow — гарячі потоки, що виходять за основний матеріал цієї лекції.
- Звичайний flow за замовчуванням виконує джерело, проміжні операції та збирача послідовно в корутині збирача. Він не створює фоновий потік автоматично.
- Emit може призупинятися; повільний collect впливає на просування звичайного послідовного джерела. Буферизація та зміна контексту потребують окремих операторів.
- Назва «потік» тут означає потік **даних**, а не обов'язково окремий потік ОС.

### Створення

| Засіб | Приклад | Для чого |
| --- | --- | --- |
| `flow { ... }` | `flow { emit(1); delay(10L); emit(2) }` | Власна логіка, очікування, emit |
| `flowOf(...)` | `flowOf(1, 2, 3)` | Відомі значення |
| `asFlow()` | `listOf("A", "B").asFlow()` | Колекція, діапазон або Sequence як джерело |
| `emptyFlow<T>()` | `emptyFlow<String>()` | Потік без елементів, зокрема для перевірки меж |

AsFlow не перетворює блокувальне джерело на неблокувальне. Sequence також не починає виконуватися паралельно лише через asFlow.

### Проміжні й термінальні операції

- **Проміжні** повертають новий Flow і описують обробку: map, transform, filter, filterNot, filterNotNull, onEach, take, drop, takeWhile, dropWhile, zip, combine.
- **Термінальні** споживають джерело: collect, toList, toSet, count, first/last/single та їх OrNull-версії, reduce, fold. У наведених прикладах вони потребують suspend-контексту.
- `onEach { ... }` додає супутню дію, але сам не запускає потік. `collect { ... }` запускає збір.
- ToList, count, last і підсумки за всім джерелом чекають його завершення. Нескінченний/гарячий потік треба обмежувати, якщо операції потрібен кінець.

```kotlin
val result = peopleFlow
    .filter { it.age >= 18 }
    .map { it.name }
    .toList()
```

Результат — List<String>. Filter/map описують обробку, toList запускає її та чекає готового списку.

### Count, take, drop та пошук

| Операція | Поведінка |
| --- | --- |
| `count()` / `count { ... }` | Кількість усіх або відповідних елементів |
| `take(n)` | Перші n елементів, потім завершує потрібний обхід upstream |
| `drop(n)` | Пропускає перші n; далі передає хвіст |
| `first()` / `first { ... }` | Перший елемент або перший відповідний; завершує пошук рано |
| `firstOrNull(...)` | Null, якщо потрібного елемента немає |
| `last()` / `lastOrNull()` | Останній елемент; потребує завершення джерела |
| `single()` | Єдиний елемент; перевіряє, що немає другого |
| `singleOrNull()` | Елемент лише при рівно одному; null при 0 або кількох |

- First/last без результату кидають NoSuchElementException. Для Flow.single без елементів — NoSuchElementException, при кількох — IllegalArgumentException.
- Single не означає «просто візьми перший»: для підтвердження єдиного значення треба побачити завершення джерела. З нескінченним потоком одного значення й відсутністю завершення він чекатиме.
- Для пошуку останнього за умовою тут використано `flow.filter { ... }.lastOrNull()`: не переносіть усі перевантаження List.last на Flow механічно.
- У Flow.take(n) n має бути **додатним**; drop(n) дозволяє 0, але не від'ємне значення. Це відрізняється від List.take(0).
- Take/first можуть достроково завершити збір джерела. У прикладі take(2) джерело емітує лише 1 і 2, а його finally виконується.

### Map, transform та фільтрація

- Map повертає одне вихідне значення на кожне вхідне: Person → String або Person → UserView.
- Transform дає доступ до emit: можна видати **0, 1 або кілька** значень для одного елемента.
- Усі емітовані значення transform мають відповідати типу вихідного Flow<R>, а не бути довільно несумісними об'єктами.
- Filter залишає елементи за предикатом; filterNot — за запереченою умовою; filterNotNull прибирає null та уточнює тип.
- TakeWhile передає початковий фрагмент до першого false і припиняє збір.
- DropWhile пропускає початковий фрагмент, **поки предикат true**. Від першого false передає весь хвіст, навіть якщо умова знову стає true.
- У прикладі віків `[37, 32, 5, 14, 25]` умова age >= 18 дає: filter → 37,32,25; takeWhile → 37,32; dropWhile → 5,14,25.

### Reduce та fold

```kotlin
val sum = (1..5).asFlow().reduce { acc, value -> acc + value } // 15
val text = flowOf("Taras", "Olena")
    .fold("Users:") { acc, name -> "$acc $name" }
```

- Reduce використовує перший елемент як накопичувач; лямбда починається з першого й другого елементів. Порожній Flow спричиняє NoSuchElementException.
- Fold має явне початкове значення й повертає його для порожнього потоку. Тип накопичувача може відрізнятися від типу елемента.
- На відміну від List.reduce, Flow.reduce — suspend-термінальна операція. Імпорт flow.* не перетворює звичайний список на Flow.

### Zip та combine

| Операція | Як поєднує |
| --- | --- |
| `first.zip(second) { a, b -> ... }` | Перше з першим, друге з другим тощо; результат обмежений коротшим джерелом |
| `first.combine(second) { a, b -> ... }` | Використовує останні відомі значення обох після того, як кожне видало хоча б одне |

- Результат може мати інший тип: String та Int → Person.
- Zip не є склеюванням усіх значень підряд і не відновлює відсутні пари.
- Combine зручний для поєднання актуальних станів. Він не гарантує окремий вихід для кожної можливої проміжної комбінації при швидких оновленнях.
- У додатковому прикладі combine затримки імітують різну швидкість; проміжні рядки залежать від планувальника. Для наведених скінченних джерел останній результат — `2:B`.

## Приклади з `tasks.kt`

Це **навчальні приклади за змістом PDF**, а не окремо сформульовані викладачем практичні завдання. Окремого переліку завдань чи контрольних запитань у PDF немає.

1. Cold flow із delay/emit/collect та повторним збором того самого джерела.
2. FlowOf, asFlow для діапазону/Sequence, toList/toSet, count/take/drop і пошук.
3. Single/OrNull та контрольована демонстрація IllegalArgumentException.
4. Map у UserView, transform із відбором і двома emit, фільтрація, takeWhile/dropWhile та onEach.
5. Reduce/fold, порожній Flow і finally джерела при take(2).
6. Zip рядків і чисел, коротше джерело, словник перекладів; додатковий combine із останніми значеннями.

## Питання для самоперевірки

Навчальні питання з короткими відповідями, додані для повторення.

1. Чи створення cold Flow запускає його тіло? Ні, потрібен збір.
2. Чи функція, що повертає flow { ... }, обов'язково suspend? Ні.
3. Що станеться при повторному collect холодного джерела? Його тіло виконається знову.
4. Чи Flow означає фоновий потік ОС? Ні.
5. Чим transform відрізняється від map? Може емітувати 0, 1 або кілька значень.
6. Чому single не дорівнює first? Single перевіряє єдиність, first бере перший і завершує пошук.
7. Чи dropWhile перевіряє весь хвіст для видалення? Ні, після першого false більше не відкидає.
8. Що fold поверне на порожньому Flow? Початкове значення.
9. Чим zip відрізняється від combine? Позиційні пари проти останніх відомих значень.
10. Чи onEach запускає збір? Ні, це проміжний оператор.


## Підключення бібліотеки та запуск із папки lecture-18

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
cd C:\Projects\RPZMP\lectures\lecture-18
"C:\Tools\kotlinc\kotlinc\bin\kotlinc.bat" tasks.kt -cp "C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar" -include-runtime -d tasks.jar
java -cp "tasks.jar;C:\Tools\kotlin-libs\kotlinx-coroutines-core-jvm-1.10.2.jar" TasksKt
```

PowerShell:

```powershell
cd C:\Projects\RPZMP\lectures\lecture-18
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

- У першому прикладі з delay потрібен `import kotlinx.coroutines.delay` або `import kotlinx.coroutines.*`; лише flow.* його не імпортує.
- Запис `Flow<>` на с. 4 неповний: потрібен `Flow<String>` або інший конкретний параметр типу.
- На с. 10 довжину >5 мають **Volodymyr (9), Kateryna (8), Stepan (6)**: count дорівнює **3**, а не 2.
- На с. 13 опис first/last суперечливий. First має версію з предикатом; для останнього за умовою тут використано filter + lastOrNull.
- На с. 14 для Flow.single з кількома елементами перевірена версія бібліотеки кидає **IllegalArgumentException**, а не IllegalStateException.
- В описі dropWhile на с. 20 переплутано умову початкового пропуску: він пропускає, поки предикат **true**, і починає передавати з першого false.
- На с. 21 перший «numberFlow» фактично є List, тому викликає колекційний reduce. Тут додано asFlow(). У прикладі userFlow.reduce потрібен suspend main/runBlocking.
- На с. 23 треба писати `english.zip(ukrainian)` і `names.zip(ages)`: імена ukr та «вік» не відповідають оголошенням.
- Виправлено розірвані ключові слова й фрагменти сигнатур на кшталт `v a l`, `suspend f u n`, `(source)`.

## Джерела

- «РПЗМП Лк 18.pdf», розділи 9.1–9.9, с. 3–24.
- [Kotlin: Flows](https://kotlinlang.org/docs/coroutines-flow.html).
- [Kotlin: Flow API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/).
- [Kotlin: Flow.single](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/single.html).
