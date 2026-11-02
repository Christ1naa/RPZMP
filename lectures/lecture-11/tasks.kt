// Лекція 11. Навчальні приклади за розділами 6.1–6.3 PDF.
import kotlin.reflect.KProperty

fun divide(first: Int, second: Int): Int = first / second

fun checkAge(age: Int): Int {
    if (age !in 1..110) {
        throw IllegalArgumentException("Вік має бути в межах 1..110: $age")
    }
    return age
}

fun checkedAgeOrNull(age: Int): Int? = try {
    checkAge(age)
} catch (e: IllegalArgumentException) {
    null
}

fun lengthOrZero(text: String?): Int = text?.length ?: 0

fun printLength(text: String?) {
    if (text != null) {
        println(text.length) // Smart cast: тут text має тип String.
    } else {
        println("Текст відсутній")
    }
}

class Profile(val name: String?)

fun displayName(profile: Profile?): String =
    profile?.name?.uppercase() ?: "Без імені"

// Окрема функція залишає можливість передати null під час виконання.
fun assertedLength(text: String?): Int = text!!.length

class ReadOnlyPerson(initialName: String) {
    val name: String by NameDelegate(initialName)
}

class NameDelegate(private val storedName: String) {
    operator fun getValue(thisRef: ReadOnlyPerson, property: KProperty<*>): String {
        println("Читання ${property.name}")
        return storedName
    }
}

class Person(val name: String, initialAge: Int) {
    var age: Int by AgeDelegate(initialAge)
}

class AgeDelegate(initialAge: Int) {
    private var storedAge: Int = initialAge

    init {
        require(initialAge in 1..110) { "Початковий вік має бути в межах 1..110" }
    }

    operator fun getValue(thisRef: Person, property: KProperty<*>): Int = storedAge

    operator fun setValue(thisRef: Person, property: KProperty<*>, value: Int) {
        // Відхилене присвоєння залишає попереднє значення, як у прикладі PDF.
        if (value in 1..110) {
            storedAge = value
        } else {
            println("${property.name}: відхилено $value")
        }
    }
}

fun main() {
    println("1. try / catch / finally та інформація про виняток")
    try {
        println(divide(10, 0))
    } catch (e: ArithmeticException) {
        println("Тип: ${e.javaClass.simpleName}")
        println("Повідомлення: ${e.message}")
        println("Перший кадр: ${e.stackTrace.firstOrNull()}")
        // e.printStackTrace() // Повна діагностика у stderr; за потреби розкоментуйте.
    } finally {
        println("finally: спробу ділення завершено")
    }
    println("Програма продовжує роботу")

    println("\n2. Кілька catch: конкретний тип перед загальним")
    try {
        val numbers = arrayOf(1, 2, 3, 4)
        println(numbers[6])
    } catch (e: ArrayIndexOutOfBoundsException) {
        println("Індекс поза межами масиву")
    } catch (e: Exception) {
        println("Інша помилка: ${e.message}")
    }

    println("\n3. throw та try як вираз")
    println(checkedAgeOrNull(5)) // 5
    println(checkedAgeOrNull(-125)) // null
    println(checkedAgeOrNull(110)) // 110: верхня межа включна.

    println("\n4. Nullable-типи, smart cast, ?. та ?:")
    printLength("Hello") // 5
    printLength(null)
    println(lengthOrZero(null)) // 0
    println(lengthOrZero("")) // 0, але рядок існує.
    println(displayName(Profile("Taras"))) // TARAS
    println(displayName(Profile(null))) // Без імені
    println(displayName(null)) // Без імені

    println("\n5. !!: успіх та контрольована демонстрація помилки")
    println(assertedLength("Kotlin")) // 6
    try {
        println(assertedLength(null))
    } catch (e: NullPointerException) {
        println("!! для null спричинив NullPointerException")
    }

    println("\n6. Делегована val-властивість")
    val taras = ReadOnlyPerson("Тарас")
    val olena = ReadOnlyPerson("Олена")
    println(taras.name)
    println(taras.name) // getValue виконується при кожному читанні.
    println(olena.name) // Окремий екземпляр делегата.

    println("\n7. Делегована var-властивість")
    val person = Person("Тарас", 37)
    println(person.age) // 37
    person.age = 38
    println(person.age) // 38
    person.age = -139
    println(person.age) // 38: некоректне присвоєння відхилено.
    person.age = 110
    println(person.age) // 110: узгоджена межа з checkAge().
}
