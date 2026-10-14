fun main() {
    task05()
}

fun task05() {
    println("Завдання 5")
    val operation1 = selectOperation("+")
    val operation2 = selectOperation("-")
    val operation3 = selectOperation("*")
    
    println(operation1(8, 5))
    println(operation2(8, 5))
    println(operation3(8, 5))
} 

fun selectOperation(type: String): (Int, Int) -> Int {
    when (type) {
        "+" -> return :: sum
        "-" -> return :: subtract
        "*" -> return :: multiply
        else -> return ::empty
    }
}

fun empty (a: Int, b: Int): Int {
    return 0
}
fun sum (a: Int, b: Int): Int {
    return a + b
}
fun subtract (a: Int, b: Int): Int {
    return a - b
}
fun multiply (a: Int, b: Int): Int {
    return a * b
}