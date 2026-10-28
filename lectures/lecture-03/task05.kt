fun main() {
    println( "Завдання 5" )
    task05()
}

fun task05() {
    println( "Введіть перше число:" )
    val number1 = readln().toDouble()
    
    println( "Введіть друге число:" )
    val number2 = readln().toDouble()
    
    println( "Введіть третє число:" )
    val number3 = readln().toDouble()
    
    when {
        number1 >= number2 && number1 >= number3 ->
            println("Найбільше число: $number1")

        number2 >= number1 && number2 >= number3 ->
            println("Найбільше число: $number2")

        else ->
            println("Найбільше число: $number3")
    }
}