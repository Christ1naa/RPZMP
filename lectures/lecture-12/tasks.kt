// Лекція 12. Навчальні приклади за розділами 6.4–6.6 PDF.

open class Person(val name: String)
open class Employee(name: String, val company: String) : Person(name)
class Manager(name: String, company: String) : Employee(name, company)

fun employment(person: Person): String =
    if (person is Employee && person.company.isNotBlank()) {
        "${person.name} працює в ${person.company}"
    } else {
        "${person.name}: місце роботи не вказано"
    }

fun companyOrNull(person: Person): String? = (person as? Employee)?.company

fun identify(person: Person): String = when (person) {
    is Manager -> "${person.name}: керівник"
    is Employee -> "${person.name}: працівник"
    else -> "${person.name}: людина"
}

fun printCompany(person: Person) {
    if (person !is Employee) return
    println(person.company) // Smart cast після раннього return.
}

open class Phone(val name: String)
class SmartPhone(name: String, val os: String) : Phone(name)
class PhoneOwner(var phone: Phone)

fun printPhone(owner: PhoneOwner) {
    // if (owner.phone is SmartPhone) println(owner.phone.os)
    // Помилка: var-властивість може змінитися між двома зверненнями.
    val phone = owner.phone
    if (phone is SmartPhone) println("${phone.name}: ${phone.os}")
}

fun String.wordCount(character: Char): Int {
    var count = 0
    for (item in this) if (item == character) count++
    return count
}

fun Int.square(): Int = this * this

// Розширення вибирається за оголошеним типом отримувача.
fun Person.role(): String = "Person"
fun Employee.role(): String = "Employee"

data class Profile(val name: String, var email: String? = null)

data class EmployeeDraft(
    var name: String = "",
    var age: Int = 0,
    var company: String = ""
) {
    fun name(value: String): EmployeeDraft = apply { name = value }
    fun age(value: Int): EmployeeDraft = apply { age = value }
    fun company(value: String): EmployeeDraft = apply { company = value }
}

class Account(var sum: Int) {
    infix fun put(amount: Int) { sum += amount }
}

// Інша назва дозволяє показати й розширення, й метод без перекриття.
infix fun Account.deposit(amount: Int) { sum += amount }
infix fun String.countChar(character: Char): Int = wordCount(character)

fun castToString(value: Any): String = value as String

fun main() {
    println("1. Перетворення значення та розбір рядка")
    println("12".toInt()) // 12
    println(12.toDouble()) // 12.0
    println(3.9.toInt()) // 3: дробова частина відкидається.
    println("taras".toIntOrNull()) // null: без винятку.
    try {
        println("taras".toInt())
    } catch (e: NumberFormatException) {
        println("Рядок не містить коректного Int")
    }

    println("\n2. is, !is, when і smart cast")
    val taras = Person("Тарас")
    val olena = Employee("Олена", "JetBrains")
    val manager = Manager("Ірина", "Kotlin Team")
    println(employment(taras))
    println(employment(olena))
    println(employment(Employee("Степан", "")))
    println(identify(manager)) // Manager перевіряється перед Employee.
    printCompany(olena)
    printPhone(PhoneOwner(SmartPhone("Pixel", "Android")))

    println("\n3. as та as?")
    println(castToString("Hello Kotlin"))
    try {
        println(castToString(12))
    } catch (e: ClassCastException) {
        println("Int не можна привести до String через as")
    }
    println(companyOrNull(taras)) // null
    println(companyOrNull(olena)) // JetBrains
    println((12 as Any) as? String) // null; toString() натомість дасть "12".

    println("\n4. Функції розширення")
    println("hello world".wordCount('l')) // 3: рахуємо символи, не слова.
    println(4.square()) // 16
    val person: Person = olena
    println(person.role()) // Person: оголошений тип посилання.
    println(olena.role()) // Employee

    println("\n5. let: it і результат лямбди")
    val profile = Profile("Taras", "taras@example.com")
    val emailLength = profile.email?.let { email ->
        println("Email: $email")
        email.length
    }
    println(emailLength) // 17
    Profile("Stepan").email?.let(::println) // Блок пропускається через ?.

    println("\n6. with і run: this і результат лямбди")
    val withResult = with(Profile("Olena")) {
        email = "${name.lowercase()}@example.com"
        email
    }
    println(withResult) // olena@example.com
    val runResult = Profile("Iryna").run {
        email = "${name.lowercase()}@example.com"
        email
    }
    println(runResult) // iryna@example.com
    val validationResult = Profile("Stepan").email?.run { "наявний" } ?: "відсутній"
    println(validationResult) // відсутній; це перевірка наявності, не формату.
    println(run { val first = 2; first + 3 }) // 5: run без отримувача.

    println("\n7. apply та also: повертають початковий об'єкт")
    val configured = Profile("Taras").apply { email = "taras@example.com" }
    val same = configured.also { println("Створено: ${it.name}") }
    println(configured === same) // true: не копія.
    val draft = EmployeeDraft().name("Олена").age(26).company("JetBrains")
    println(draft)

    println("\n8. Інфіксна нотація")
    val account = Account(1000)
    account put 150
    println(account.sum) // 1150
    account.put(150)
    println(account.sum) // 1300: другий виклик додає ще раз.
    account deposit 50
    println(account.sum) // 1350: інфіксне розширення.
    println("hello world" countChar 'o') // 2
    println("UA" to "Україна") // Стандартна infix-функція створює Pair.
}
