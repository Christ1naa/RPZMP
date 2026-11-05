// Лекція 14. Навчальні приклади за розділами 7.6–7.10 PDF.
data class Person(val name: String, val age: Int)

open class Contact(val name: String) {
    override fun toString(): String = name
}
class Employee(name: String, val company: String) : Contact(name)

fun main() {
    val people = listOf(Person("Taras", 37), Person("Stepan", 25), Person("Olena", 33))

    println("1. List: операції виконуються поетапно одразу")
    val eager = people
        .filter { println("list age: ${it.name}"); it.age > 30 }
        .filter { println("list name: ${it.name}"); it.name.length == 5 }
    println("Результат List: $eager") // Taras та Olena.

    println("\n2. Sequence: обробка починається під час обходу")
    val lazy = people.asSequence()
        .filter { println("sequence age: ${it.name}"); it.age > 30 }
        .filter { println("sequence name: ${it.name}"); it.name.length == 5 }
    println("Ланцюжок створено; фільтри ще не виконувалися")
    for (person in lazy) println("Результат: $person")

    println("\n3. take(1): скільки елементів справді перевірено")
    var eagerChecks = 0
    val eagerFirst = people.filter { eagerChecks++; it.age > 30 }.take(1)
    var lazyChecks = 0
    val lazyFirst = people.asSequence().filter { lazyChecks++; it.age > 30 }.take(1).toList()
    println("List: $eagerFirst; перевірок: $eagerChecks") // 3
    println("Sequence: $lazyFirst; перевірок: $lazyChecks") // 1
    // Для отримання одного результату іноді достатньо прямого пошуку.
    println(people.firstOrNull { it.age > 30 })

    println("\n4. filter, filterNot, filterIndexed і тип результату")
    val names = listOf("Taras", "Myhailo", "Stepan", "Volodymyr", "Olena")
    println(names.filter { it.length == 5 }) // [Taras, Olena]
    println(names.filterNot { it.length == 5 }) // [Myhailo, Stepan, Volodymyr]
    println(names.filterIndexed { index, name -> index % 2 == 0 && name.length == 5 })
    val fromSet: List<Int> = setOf(1, 2, 3).filter { it > 1 }
    println(fromSet) // [2, 3], результат саме List.
    val ages = mapOf("Taras" to 37, "Stepan" to 25)
    val older: Map<String, Int> = ages.filter { (_, age) -> age > 30 }
    println(older) // {Taras=37}, результат саме Map.

    println("\n5. Фільтрація за типом та null")
    val contacts: List<Contact> = listOf(
        Contact("Taras"), Employee("Olena", "Microsoft"), Employee("Stepan", "Google")
    )
    val employees: List<Employee> = contacts.filterIsInstance<Employee>()
    println(employees.map { "${it.name}: ${it.company}" })
    val nullable: List<String?> = listOf("Taras", null, "Olena", null)
    val present: List<String> = nullable.filterNotNull()
    println(present.map { it.length }) // [5, 5]: !! не потрібен.

    println("\n6. all, any, none та перевірка належності")
    println(names.all { it.length == 5 }) // false
    println(names.any { it.length == 5 }) // true
    println(names.none { it.length == 4 }) // true
    println(names.any()) // true
    println(names.none()) // false
    println("Stepan" in names) // true: аналог contains.
    println(names.containsAll(listOf("Taras", "Olena"))) // true
    val empty = emptyList<String>()
    println("Порожній: all=${empty.all { it.length == 5 }}, any=${empty.any { it.length == 5 }}, none=${empty.none { it.length == 5 }}")
    // all=true, any=false, none=true: немає елемента, який порушує умову all.

    println("\n7. map, mapIndexed та відкидання null-результатів")
    println(people.map { it.name })
    println((1..5).map { it * it }) // [1, 4, 9, 16, 25]
    println(people.mapIndexed { index, person -> "${index + 1}.${person.name}" })
    println(listOf("12", "bad", "7").mapNotNull { it.toIntOrNull() }) // [12, 7]
    println(names.mapIndexedNotNull { index, name -> if (index % 2 == 0) null else name })
    // [Myhailo, Volodymyr]: індекс береться у вхідному списку.

    println("\n8. flatten: один рівень вкладеності, дублікати залишаються")
    val teams = listOf(listOf("Taras", "Olena"), listOf("Stepan", "Taras"))
    println(teams.flatten()) // [Taras, Olena, Stepan, Taras]
    println(sequenceOf(sequenceOf(1, 2), sequenceOf(3)).flatten().toList())

    println("\n9. groupBy: повні об'єкти або перетворені значення")
    val staff = listOf(
        Employee("Taras", "Microsoft"), Employee("Volodymyr", "JetBrains"),
        Employee("Stepan", "Google"), Employee("Olena", "Microsoft")
    )
    val byCompany: Map<String, List<Employee>> = staff.groupBy { it.company }
    for ((company, members) in byCompany) println("$company: ${members.map { it.name }}")
    val companyNames: Map<String, List<String>> = staff.groupBy({ it.company }) { it.name }
    println(companyNames)
    println(staff.asSequence().groupBy { it.company }.mapValues { it.value.size })
    // groupBy для Sequence — термінальна операція, яка створює Map.
}
