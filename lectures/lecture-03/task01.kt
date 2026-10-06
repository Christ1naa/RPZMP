fun main() {
    println("Завдання 1")
    task01()
}

fun task01() {
    println("Введіть перше значення: ")
    val number1 = readln().toDouble()
    
    println("Введіть друге значення: ")
    val number2 = readln().toDouble()
    
    println("Введіть дію (+, -, *, /): ")
    val action = readln()
    
    when (action) {
        "+" -> println("$number1 + $number2 = ${number1 + number2}")
        "-" -> println("$number1 - $number2 = ${number1 - number2}")
        "*" -> println("$number1 * $number2 = ${number1 * number2}")
        "/" -> {
            if (number2 == 0.0) {
                println(" Ділити на нуль не можна")
            } else {
                println("$number1 / $number2 = ${number1/number2}")
            }
        }
        else -> println("Неправильна дія") 
    }
}          