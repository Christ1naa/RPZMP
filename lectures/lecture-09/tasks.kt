// Лекція 9. Навчальні приклади за розділами 4.12–4.14 PDF.

enum class Day(val number: Int) {
    MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4),
    FRIDAY(5), SATURDAY(6), SUNDAY(7);

    // Різниця номерів у межах одного тижня, без переходу через неділю.
    fun differenceFrom(other: Day): Int = number - other.number
}

interface Printable {
    fun printName()
}

enum class DayTime : Printable {
    DAY {
        override val startHour: Int = 6
        override val endHour: Int = 21
        override fun printName() = println("День")
    },
    NIGHT {
        override val startHour: Int = 22
        override val endHour: Int = 5
        override fun printName() = println("Ніч")
    };

    abstract val startHour: Int
    abstract val endHour: Int
}

enum class Operation { ADD, SUBTRACT, MULTIPLY }

fun operate(first: Int, second: Int, operation: Operation): Int = when (operation) {
    Operation.ADD -> first + second
    Operation.SUBTRACT -> first - second
    Operation.MULTIPLY -> first * second
}

interface Messenger {
    val programName: String
    fun send(message: String)
    fun sendVideo()
}

class InstantMessenger(override val programName: String) : Messenger {
    override fun send(message: String) = println("$programName: $message")
    override fun sendVideo() = println("$programName: надіслано відео")
}

interface PhotoDevice {
    fun takePhoto()
}

class PhotoCamera : PhotoDevice {
    override fun takePhoto() = println("Фото зроблено")
}

class SmartPhone(val name: String, messenger: Messenger, camera: PhotoDevice) :
    Messenger by messenger, PhotoDevice by camera

class CustomPhone(messenger: Messenger) : Messenger by messenger {
    override val programName: String = "SMS"
    override fun send(message: String) = println("$programName: $message")
}

open class Person(val name: String) {
    open fun sayHello() = println("Привіт, я $name")
}

fun hello(person: Person) = person.sayHello()

// Приватна функція зі виведеним типом зберігає власні члени анонімного об'єкта.
private fun createProfile(name: String, company: String) = object {
    val name: String = name
    val company: String = company
    fun display() = println("$name працює в $company")
}

// Явний тип Person визначає, які члени бачить код, що викликає функцію.
fun createPerson(name: String): Person = object : Person(name) {
    val company: String = "JetBrains"
    override fun sayHello() = println("Привіт, я $name з $company")
}

fun main() {
    println("1. Перерахування")
    val friday = Day.FRIDAY
    println("${friday.name}: ordinal = ${friday.ordinal}, number = ${friday.number}")
    println("Різниця з понеділком: ${friday.differenceFrom(Day.MONDAY)}")
    println(Day.values().joinToString())
    println(Day.valueOf("FRIDAY"))

    println("\n2. Поведінка констант та when")
    DayTime.DAY.printName()
    DayTime.NIGHT.printName()
    println("Ніч: ${DayTime.NIGHT.startHour}:00–${DayTime.NIGHT.endHour}:00")
    println(operate(5, 6, Operation.ADD))
    println(operate(5, 6, Operation.SUBTRACT))
    println(operate(5, 6, Operation.MULTIPLY))

    println("\n3. Делегування кільком об'єктам")
    val telegram = InstantMessenger("Telegram")
    val phone = SmartPhone("Pixel", telegram, PhotoCamera())
    println("${phone.name}: ${phone.programName}")
    phone.send("Вивчаю Kotlin")
    phone.takePhoto()

    println("\n4. Власні реалізації замість делегування")
    val customPhone = CustomPhone(telegram)
    println(customPhone.programName)
    customPhone.send("Привіт")
    customPhone.sendVideo() // Делегат використовує власне programName: Telegram.

    println("\n5. Анонімні об'єкти")
    val localUser = object {
        val name: String = "Тарас"
        fun sayHello() = println("Привіт, я $name")
    }
    println(localUser.name)
    localUser.sayHello()
    hello(object : Person("Степан") {
        override fun sayHello() = println("$name: одноразове перевизначення")
    })
    val profile = createProfile("Олена", "JetBrains")
    println(profile.company)
    profile.display()
    val person = createPerson("Ірина")
    println(person.name)
    person.sayHello()
    // person.company недоступна: оголошений тип результату — Person.
}
