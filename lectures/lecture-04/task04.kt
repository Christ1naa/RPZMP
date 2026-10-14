fun main() {
    task04()
}

fun task04() {
    println("Завдання 4")
    multiply(2, 3, 4)
    multiply(5, 2)
}

fun multiply(vararg numbers: Int) {
    var result = 1
    for (number in numbers) {
        result *= number
    }
    println("Добуток: $result")
}  