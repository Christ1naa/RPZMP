fun main() {
    task01()
}

fun task01() {
    println("Завдання 1")
    println("Введіть ім'я:")
    val name = readln()
    greet(name)
}

fun greet(name: String) {
    println("Привіт, $name!")
}