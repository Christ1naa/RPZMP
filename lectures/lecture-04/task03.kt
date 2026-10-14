fun main() {
    task03()
}

fun task03() {
    println("Завдання 3")
    printUserInfo("Андрій", 22)
    printUserInfo("Олексій")
}

fun printUserInfo(name: String, age: Int = 18) {
    println("Ім'я: $name, вік: $age")
}