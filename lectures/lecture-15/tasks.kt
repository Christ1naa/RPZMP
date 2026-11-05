// Лекція 15. Навчальні приклади за розділами 7.11–7.15 PDF.
import kotlin.random.Random

class Person(val name: String, val age: Int) : Comparable<Person> {
    // Природний порядок цього класу — за ім'ям.
    override fun compareTo(other: Person): Int = name.compareTo(other.name)
    override fun toString(): String = "$name ($age)"
}

fun main() {
    val people = listOf(Person("Taras", 37), Person("Volodymyr", 41), Person("Stepan", 25))

    println("1. Comparable: природний порядок")
    println(people.sorted()) // [Stepan (25), Taras (37), Volodymyr (41)]
    println(people.sortedDescending()) // [Volodymyr (41), Taras (37), Stepan (25)]
    println("Taras".compareTo("Volodymyr") < 0) // true

    println("\n2. Comparator та селектори")
    val byAge = Comparator<Person> { first, second -> first.age.compareTo(second.age) }
    println(people.sortedWith(byAge)) // Stepan, Taras, Volodymyr
    println(people.sortedWith(compareBy { it.age }))
    println(people.sortedBy { it.name }) // Stepan, Taras, Volodymyr
    println(people.sortedByDescending { it.age }) // Volodymyr, Taras, Stepan
    val tied = listOf(Person("Taras", 30), Person("Olena", 30), Person("Stepan", 25))
    println(tied.sortedWith(compareBy<Person> { it.age }.thenBy { it.name }))
    // Додатковий критерій thenBy для однакового віку.
    println(listOf("Taras", "Olena", "Kateryna", "Stepan", "Volodymyr").sortedBy { it.length })
    // [Taras, Olena, Stepan, Kateryna, Volodymyr]

    println("\n3. Зворотний та випадковий порядок")
    val source = listOf(1, 2, 3, 4, 5, 6)
    println(source.reversed()) // [6, 5, 4, 3, 2, 1]
    val shuffled = source.shuffled(Random(42))
    println(shuffled) // Порядок випадковий; seed спрощує відтворення прикладу.
    println(shuffled.sorted() == source) // true: склад не змінився.
    println(source) // Початковий список незмінний.
    val editable = source.toMutableList()
    editable.reverse() // Змінює цей MutableList.
    println(editable)
    editable.shuffle(Random(42)) // Також змінює наявний список.
    println(editable.sorted() == source) // true

    println("\n4. Мінімум/максимум: об'єкт чи значення ознаки")
    val numbers = listOf(4, 6, 3, 5, 1, 2)
    println(numbers.minOrNull()) // 1
    println(numbers.maxOrNull()) // 6
    println(people.minByOrNull { it.age }) // Stepan (25)
    println(people.maxByOrNull { it.age }) // Volodymyr (41)
    println(people.minOfOrNull { it.age }) // 25
    println(people.maxOfOrNull { it.age }) // 41
    val colors = listOf("red", "green", "blue", "yellow")
    println(colors.minWithOrNull(compareBy { it.length })) // red
    println(colors.maxWithOrNull(compareBy { it.length })) // yellow
    val byLength = compareBy<String> { it.length }
    println(people.minOfWithOrNull(byLength) { it.name }) // Taras: перший із найкоротших.
    println(people.maxOfWithOrNull(byLength) { it.name }) // Volodymyr

    println("\n5. Агрегати, reduce і fold")
    println(numbers.average()) // 3.5
    println(numbers.sum()) // 21
    println(people.sumOf { it.age }) // 103
    println(people.count()) // 3
    println(people.count { it.age > 30 }) // 2
    println((1..5).reduce { accumulator, item -> accumulator + item }) // 15
    println(people.map { it.name }.reduce { accumulator, name -> "$accumulator $name" })
    println(people.fold("People:") { accumulator, person -> "$accumulator ${person.name}" })
    println(emptyList<Int>().fold(0) { accumulator, item -> accumulator + item }) // 0
    println(emptyList<Int>().reduceOrNull { accumulator, item -> accumulator + item }) // null
    println(emptyList<Int>().minOrNull()) // null
    println(emptyList<Int>().average().isNaN()) // true

    println("\n6. plus/minus: новий результат, вихідні дані збережені")
    val names = listOf("Taras", "Stepan", "Taras", "Olena")
    println(names + "Kateryna")
    println(names + listOf("Iryna", "Myhailo"))
    println(names - "Taras") // [Stepan, Taras, Olena]: лише перше входження.
    println(names - listOf("Taras")) // [Stepan, Olena]: усі входження набору.
    println(names) // Початкові дублікати залишаються.

    println("\n7. zip, перетворення пар та unzip")
    val english = listOf("red", "blue", "green")
    val ukrainian = listOf("червоний", "синій", "зелений")
    val pairs = english.zip(ukrainian)
    for ((first, second) in pairs) println("$first: $second")
    println(english.zip(ukrainian) { en, ua -> "$en — $ua" })
    println(english.zip(listOf(1, 2))) // green не має пари і не потрапить у результат.
    val (left, right) = pairs.unzip()
    println(left)
    println(right)

    println("\n8. slice: індекси, діапазони, дублювання")
    val letters = listOf("a", "b", "c", "d", "e", "f")
    println(letters.slice(1..3)) // [b, c, d]
    println(letters.slice(0..5 step 2)) // [a, c, e]
    println(letters.slice(listOf(1, 3, 5, 1))) // [b, d, f, b]

    println("\n9. take/drop та їхні While/Last-версії")
    val values = listOf(1, 2, 9, 3, 4)
    println(values.take(2)) // [1, 2]
    println(values.takeLast(2)) // [3, 4]
    println(values.drop(2)) // [9, 3, 4]
    println(values.dropLast(2)) // [1, 2, 9]
    println(values.takeWhile { it < 5 }) // [1, 2]
    println(values.dropWhile { it < 5 }) // [9, 3, 4]
    println(values.takeLastWhile { it < 5 }) // [3, 4]
    println(values.dropLastWhile { it < 5 }) // [1, 2, 9]
    println(values.filter { it < 5 }) // [1, 2, 3, 4]: порівняйте з takeWhile.
    println(values.take(99)) // Усі наявні елементи.

    println("\n10. chunked: блоки й обчислення над ними")
    println(letters.chunked(4)) // [[a, b, c, d], [e, f]]
    println(values.chunked(2) { it.sum() }) // [3, 12, 4]
    println(values.asSequence().chunked(2).toList()) // [[1, 2], [9, 3], [4]]

    println("\n11. Окремі елементи та безпечні альтернативи")
    println(letters.elementAt(1)) // b
    println(letters.elementAtOrNull(99)) // null
    println(letters.elementAtOrElse(99) { "Індекс $it поза межами" })
    println(values.first { it < 5 }) // 1
    println(values.last { it < 5 }) // 4
    println(values.firstOrNull { it > 100 }) // null
    println(emptyList<Int>().lastOrNull()) // null
    println(letters.random(Random(42))) // Один із наявних елементів.
    println(emptyList<String>().randomOrNull()) // null

    println("\n12. Sequence: відбір перед сортуванням")
    println(generateSequence(1) { it + 1 }.take(5).sortedDescending().toList())
    // [5, 4, 3, 2, 1]: спочатку обмежуємо нескінченне джерело.
}
