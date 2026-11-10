// Лекція 18. Навчальні приклади за розділами 9.1–9.9 PDF.
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class Person(val name: String, val age: Int)
data class UserView(val name: String, val isAdult: Boolean)

fun usersFlow(): Flow<String> = flow {
    println("Тіло flow почало виконуватися")
    for (name in listOf("Taras", "Volodymyr", "Stepan")) {
        delay(20L)
        println("emit: $name")
        emit(name)
    }
}

suspend fun coldFlowExample() {
    val users = usersFlow()
    println("Об'єкт Flow створено; значення ще не емітувалися")
    users.collect { println("collect: $it") }
    println("Повторний збір запускає cold flow заново")
    println(users.toList())
}

suspend fun buildersAndTerminals() {
    println(flowOf(1, 2, 3).toList())
    println((1..3).asFlow().toSet())
    println(sequenceOf("A", "B").asFlow().toList())
    val names = listOf("Taras", "Volodymyr", "Kateryna", "Stepan", "Olena").asFlow()
    println(names.count()) // 5
    println(names.count { it.length > 5 }) // 3, не 2.
    println(names.take(3).toList()) // Taras, Volodymyr, Kateryna
    println(names.drop(3).toList()) // Stepan, Olena
    println(names.first()) // Taras
    println(names.first { it.length > 8 }) // Volodymyr
    println(names.firstOrNull { it.length > 20 }) // null
    println(names.last()) // Olena
    println(names.filter { it.length > 5 }.lastOrNull()) // Stepan
    println(emptyFlow<String>().lastOrNull()) // null
    println(flowOf("Only").single()) // Only
    println(emptyFlow<String>().singleOrNull()) // null
    println(flowOf("A", "B").singleOrNull()) // null
    try {
        flowOf("A", "B").single()
    } catch (e: IllegalArgumentException) {
        println("single: більше одного елемента → ${e.javaClass.simpleName}")
    }
}

suspend fun transformations() {
    val people = listOf(
        Person("Taras", 37), Person("Olena", 32), Person("Vadym", 5),
        Person("Stepan", 14), Person("Volodymyr", 25)
    ).asFlow()
    println(people.map { it.name }.toList())
    println(people.map { UserView(it.name, it.age >= 18) }.toList())
    println(people.transform { if (it.age >= 18) emit(it.name) }.toList())
    println(flowOf(2, 3, 4).transform { emit(it); emit(it * it) }.toList())
    // [2, 4, 3, 9, 4, 16]: transform може видати 0, 1 або кілька значень.
    println(people.filter { it.age >= 18 }.map { it.name }.toList())
    println(people.filterNot { it.age >= 18 }.map { it.name }.toList())
    println(flowOf("A", null, "B").filterNotNull().toList())
    println(people.takeWhile { it.age >= 18 }.map { it.name }.toList()) // Taras, Olena
    println(people.dropWhile { it.age >= 18 }.map { it.name }.toList()) // Vadym, Stepan, Volodymyr
    flowOf(1, 2).onEach { println("onEach: $it") }.collect { println("collect: $it") }
}

suspend fun reductionsAndCancellation() {
    println((1..5).asFlow().reduce { acc, number -> acc + number }) // 15
    println(flowOf("Taras", "Olena").fold("Users:") { acc, name -> "$acc $name" })
    println(emptyFlow<Int>().fold(0) { acc, number -> acc + number }) // 0
    try {
        emptyFlow<Int>().reduce { acc, number -> acc + number }
    } catch (e: NoSuchElementException) {
        println("reduce порожнього Flow: NoSuchElementException")
    }
    val source = flow {
        try {
            for (number in 1..5) {
                println("Джерело: $number")
                emit(number)
            }
        } finally {
            println("finally джерела після take(2)")
        }
    }
    println(source.take(2).toList()) // [1, 2]; 3..5 не емітуються.
}

suspend fun combineExamples() {
    val names = flowOf("Taras", "Olena", "Stepan")
    val ages = flowOf(37, 32)
    println(names.zip(ages) { name, age -> Person(name, age) }.toList())
    // Дві пари: зайве ім'я Stepan не має відповідного віку.
    val english = flowOf("red", "yellow", "blue")
    val ukrainian = flowOf("червоний", "жовтий", "синій")
    println(english.zip(ukrainian) { en, ua -> "$en: $ua" }.toList())

    // Додатковий приклад до переліку combine у §9.3.2 PDF.
    val numberUpdates = flow { emit(1); delay(40L); emit(2) }
    val letterUpdates = flow { emit("A"); delay(70L); emit("B") }
    numberUpdates.combine(letterUpdates) { number, letter -> "$number:$letter" }
        .collect { println("combine, останні значення: $it") }
    // Точний набір проміжних комбінацій залежить від часу надходження.
    // Остання комбінація цих скінченних джерел — 2:B.
}

fun main() = runBlocking {
    println("1. Cold Flow, emit і collect")
    coldFlowExample()
    println("\n2. Побудова й термінальні операції")
    buildersAndTerminals()
    println("\n3. Перетворення та фільтрація")
    transformations()
    println("\n4. reduce/fold та завершення джерела через take")
    reductionsAndCancellation()
    println("\n5. zip та combine")
    combineExamples()
}
