// Лекція 6. Навчальні приклади: класи, конструктори, init та успадковування.

open class Person(val name: String, var age: Int) {
    init {
        require(name.isNotBlank()) { "Ім'я не може бути порожнім" }
        require(age >= 0) { "Вік не може бути від'ємним" }
    }

    constructor(name: String) : this(name, 18)

    fun printInfo() {
        println("Ім'я: $name, вік: $age")
    }
}

class Employee(name: String, age: Int, val company: String) : Person(name, age) {
    fun printCompany() {
        println("$name працює у компанії $company")
    }
}

fun main() {
    val person = Person("Олена")
    person.printInfo()
    person.age = 21
    person.printInfo()

    val employee = Employee("Тарас", 27, "JetBrains")
    employee.printInfo()
    employee.printCompany()
}
