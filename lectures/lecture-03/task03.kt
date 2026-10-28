fun main() {
    println("Завдання 3")
    task03()
}

fun task03() {
    val range = 1..20
    println("Парні числа від 1 до 20:")

    for (i in range) {
        if (i % 2 == 0) {
            println(i)
        }
    }
}