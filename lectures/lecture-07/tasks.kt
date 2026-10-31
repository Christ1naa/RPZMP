// Лекція 7. Навчальні приклади: видимість, аксесори та override.

open class Person(private val firstName: String, private val lastName: String) {
    protected val name: String
        get() = "$firstName $lastName"

    open var age: Int = 18
        set(value) {
            if (value in 1..109) {
                field = value
            }
        }

    open val fullInfo: String
        get() = "$name, вік: $age"

    open fun display() {
        println(fullInfo)
    }
}

open class Employee(
    firstName: String,
    lastName: String,
    val company: String
) : Person(firstName, lastName) {
    override var age: Int = 18
        set(value) {
            if (value in 18..109) {
                field = value
            }
        }

    override val fullInfo: String
        get() = "${super.fullInfo}, компанія: $company"

    override fun display() {
        super.display()
        println("Посада: працівник")
    }
}

class Manager(
    firstName: String,
    lastName: String,
    company: String
) : Employee(firstName, lastName, company) {
    override fun display() {
        println("Менеджер: $fullInfo")
    }
}

fun main() {
    val person = Person("Олена", "Коваль")
    person.age = -5  // Некоректне значення сетер ігнорує.
    person.display()

    val employee = Employee("Тарас", "Шевченко", "JetBrains")
    employee.age = 15  // У працівника мінімальний вік 18 років.
    employee.age = 27
    employee.display()

    val manager: Person = Manager("Ірина", "Бондар", "Example Ltd")
    manager.display() // Викликається перевизначення з Manager.
}
