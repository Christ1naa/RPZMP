fun main() {
    task02()
}

fun task02() {
    println("Введіть перше число:")
    val a = readln().toInt()
    println("Введіть друге число:")
    val b = readln().toInt()

    val result = sum(a, b)
    println("Сума $a та $b дорівнює $result")
}

fun sum(a: Int, b: Int): Int {
    return a + b
}