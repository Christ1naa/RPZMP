fun main() {
    println("Завдання 1")
    task01()

    println("\nЗавдання 2")
    task02()

    println("\nЗавдання 3")
    task03()

    println("\nЗавдання 4")
    task04()

    println("\nЗавдання 5")
    task05()
}


    fun task01() {
        println("Привіт, Kotlin!")
    }

    fun task02() {
        val name: String = "Андрій"
        val age: Int = 25

        println("Мене звати $name, мені $age років")
    }

    fun task03(){
        print("Введіть перше число: ")
        val number1 = readln().toDouble()

        print("Введіть друге число: ")
        val number2 = readln().toDouble()

        val sum = number1 + number2
        val difference = number1 - number2
        val product = number1 * number2

        println("Сума чисел $number1 і $number2: $sum")
        println("Різниця чисел $number1 і $number2: $difference")
        println("Добуток чисел $number1 і $number2: $product")

        if (number2 == 0.0) {
            println("Частку обчислити неможливо: ділити на нуль не можна")
        } else {
            val quotient = number1 / number2
            println("Частка чисел $number1 і $number2: $quotient")
        }
    }

    fun task04(){
        print("Введіть число: ")
        val number = readln().toDouble()

        println("Додатне: ${number > 0}")
        println("Від’ємне: ${number < 0}")
        println("Нуль: ${number == 0.0}")
    }

    fun task05() {
        print("Введіть ціле число: ")
        val number = readln().toInt()

        val isEven: Boolean = number %2 == 0
        println("Число парне: $isEven")
    }