class Car(val brand: String, val year: Int) {
    constructor(brand: String) : this(brand = brand, year = 2026)
}

fun main(){
    val car1 = Car("Toyota", 2020)
    val car2 = Car("Audi")
    val car3 = Car("BMW", 2024)
    val car4 = Car("Ford")
    
    println("Автомобілі:")
    println("1. Марка: ${car1.brand}, рік: ${car1.year}")
    println("2. Марка: ${car2.brand}, рік: ${car2.year}")
    println("3. Марка: ${car3.brand}, рік: ${car3.year}")
    println("4. Марка: ${car4.brand}, рік: ${car4.year}")
}