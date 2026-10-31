// Лекція 8. Навчальні приклади за розділами 4.8–4.11 PDF.

abstract class Figure(val name: String) {
    abstract val area: Double
    abstract fun perimeter(): Double

    fun display() {
        println("$name: площа = $area, периметр = ${perimeter()}")
    }
}

class Rectangle(val width: Double, val height: Double) : Figure("Прямокутник") {
    override val area: Double
        get() = width * height

    override fun perimeter(): Double = 2 * (width + height)
}

interface Movable {
    var speed: Int
    fun move()
    fun stop() = println("Зупинився")
}

interface Info {
    val model: String
        get() = "Невідома модель"
    val number: String
}

class Car(override val model: String, override var number: String) : Movable, Info {
    override var speed: Int = 60
    override fun move() = println("$model їде зі швидкістю $speed км/год")
}

class Aircraft : Movable {
    override var speed: Int = 600
    override fun move() = println("Літак летить зі швидкістю $speed км/год")
    override fun stop() = println("Літак приземлився")
}

open class Video {
    open fun play() = println("Відтворення відео")
}

interface AudioPlayable {
    fun play() = println("Відтворення аудіо")
}

class MediaPlayer : Video(), AudioPlayable {
    override fun play() {
        // Обираємо обидві реалізації явно, бо їхні сигнатури збігаються.
        super<Video>.play()
        super<AudioPlayable>.play()
    }
}

class BankAccount(private var sum: Int) {
    class Details(val owner: String)
    interface Receipt {
        fun printReceipt()
    }

    fun display() = println("Залишок: $sum")

    inner class Transaction(private val sum: Int) {
        fun pay() {
            require(sum > 0 && sum <= this@BankAccount.sum) {
                "Сума платежу має бути додатною та не перевищувати залишок"
            }
            // sum належить платежу, this@BankAccount.sum — рахунку.
            this@BankAccount.sum -= this@Transaction.sum
            display()
        }
    }
}

interface Container {
    class Item(val title: String)
    interface Printable {
        fun print()
    }
}

data class Person(val name: String, val age: Int) {
    // Не бере участі в згенерованих equals/hashCode/toString/copy/componentN.
    var city: String = "Не вказано"
}

fun main() {
    println("1. Абстрактний клас")
    val figure: Figure = Rectangle(3.0, 4.0)
    figure.display()

    println("\n2. Інтерфейси")
    val car = Car("Tesla", "AA1234BB")
    println("${car.model}, номер: ${car.number}")
    val vehicle: Movable = car
    vehicle.move()
    vehicle.stop()
    val aircraft: Movable = Aircraft()
    aircraft.move()
    aircraft.stop()

    println("\n3. Конфлікт реалізацій")
    MediaPlayer().play()

    println("\n4. Вкладені та внутрішні класи")
    val details = BankAccount.Details("Олена")
    println("Власниця: ${details.owner}")
    val account = BankAccount(3400)
    account.Transaction(2400).pay()
    println(Container.Item("Навчальний приклад").title)

    println("\n5. Data-класи")
    val olena = Person("Олена", 24)
    olena.city = "Київ"
    val sameData = Person("Олена", 24)
    sameData.city = "Львів"
    println(olena)
    println("Рівність за даними конструктора: ${olena == sameData}")
    println("Той самий об'єкт: ${olena === sameData}")
    println("Однакові хеш-коди: ${olena.hashCode() == sameData.hashCode()}")
    val kateryna = olena.copy(name = "Катерина")
    println(kateryna)
    println("Місто копії: ${kateryna.city}")
    val (name, age) = olena
    println("Ім'я: $name, вік: $age")
}
