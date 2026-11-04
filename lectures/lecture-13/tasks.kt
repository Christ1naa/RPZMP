// Лекція 13. Навчальні приклади за розділами 7.1–7.5 PDF.

data class Document(val title: String, val type: String, val amount: Int)

fun main() {
    println("1. Read-only та mutable: val не заморожує вміст")
    val editable = mutableListOf(1, 2)
    val readOnly: List<Int> = editable
    editable.add(3)
    println(readOnly) // [1, 2, 3]: те саме сховище.
    val snapshot = editable.toList()
    editable.add(4)
    println(snapshot) // [1, 2, 3]: окремий поверхневий знімок.
    println(editable) // [1, 2, 3, 4]
    // readOnly.add(5) // Немає методу add у контракті List.

    println("\n2. List та MutableList")
    val numbers = listOf(1, 2, 3, 4, 5)
    for (number in numbers) print("$number ")
    println()
    println(numbers[1]) // 2
    println(numbers.indexOf(2)) // 1
    println(numbers.lastIndexOf(3)) // 2
    println(numbers.first()) // 1
    println(numbers.last()) // 5
    println(numbers.size) // 5
    println(numbers.contains(4)) // true
    println(numbers.elementAtOrNull(9)) // null
    val list = mutableListOf(5, 6, 7)
    list.add(12)
    list.add(0, 23)
    list.addAll(0, listOf(-3, -2, -1))
    list.removeAt(0) // Видаляє елемент за індексом.
    list.remove(5) // Видаляє перше значення 5.
    list[0] = 99
    println(list) // [99, -1, 23, 6, 7, 12]
    val extended = numbers + 6
    println(extended) // [1, 2, 3, 4, 5, 6]
    println(numbers) // Початковий список не змінився.

    println("\n3. Set та MutableSet")
    val unique = setOf(1, 1, 2, 3)
    println(unique.size) // 3
    println(unique + 4)
    println(unique - 2)
    println(unique intersect setOf(2, 4)) // [2]
    println(unique union setOf(3, 4)) // [1, 2, 3, 4]
    println(unique subtract setOf(2, 4)) // [1, 3]
    val tags = linkedSetOf("документ", "рахунок")
    println(tags.add("документ")) // false: дублікат.
    tags.add("важливе")
    tags.remove("рахунок")
    println(tags) // Порядок додавання зберігається LinkedHashSet.
    val unordered = hashSetOf(3, 1, 2)
    println(unordered.contains(2)) // true; порядок HashSet не перевіряємо.

    println("\n4. Map та MutableMap")
    val codes = mapOf("UA" to "Україна", "NL" to "Нідерланди")
    println(codes["UA"])
    println(codes["XX"]) // null: відсутній ключ.
    println(codes.getOrDefault("XX", "Невідомо"))
    for ((key, value) in codes) println("$key — $value")
    val counts = mutableMapOf("documents" to 2, "bills" to 1)
    counts["bills"] = 3 // Оновлення за наявним ключем.
    counts.put("paid", 1)
    counts.remove("documents")
    println(counts) // {bills=3, paid=1}
    println(mapOf("A" to 1, "A" to 2)) // {A=2}: останнє значення ключа.

    println("\n5. Перетворення, пошук, групування та агрегування")
    val docs = listOf(
        Document("Паспорт", "document", 0),
        Document("Інтернет", "bill", 30),
        Document("Електрика", "bill", 50)
    )
    println(docs.filter { it.type == "bill" }.map { it.title })
    println(docs.find { it.amount > 40 }?.title) // Електрика
    println(docs.firstOrNull { it.amount > 100 }) // null
    println(docs.any { it.type == "bill" }) // true
    println(docs.all { it.amount >= 0 }) // true
    println(docs.count { it.type == "bill" }) // 2
    println(docs.groupBy { it.type }.mapValues { it.value.size })
    println(docs.sortedByDescending { it.amount }.map { it.title })
    println(docs.sumOf { it.amount }) // 80
    println(docs.maxOfOrNull { it.amount }) // 50
    println(numbers.fold(0) { total, value -> total + value }) // 15
    println(numbers.reduce { total, value -> total + value }) // 15
    println(emptyList<Int>().fold(0) { total, value -> total + value }) // 0
    println(listOf("1", "bad", "3").mapNotNull { it.toIntOrNull() }) // [1, 3]
    println(listOf(1, null, 2).filterNotNull()) // [1, 2]
    println(listOf(1, 1, 2, 3).distinct()) // [1, 2, 3]
    println(docs.distinctBy { it.type }.map { it.title }) // Паспорт, Інтернет
    println(listOf(listOf(1, 2), listOf(3)).flatMap { it }) // [1, 2, 3]
    println(numbers.chunked(2)) // [[1, 2], [3, 4], [5]]
    println(numbers.mapIndexed { index, value -> "$index:$value" })
    println(listOf(1, 3, 2, 4).takeWhile { it < 3 }) // [1]: до першої невдачі.
    println(listOf(1, 3, 2, 4).filter { it < 3 }) // [1, 2]: перевірка всіх.

    println("\n6. Чотири способи створення Sequence")
    println(sequenceOf("Тарас", "Олена").joinToString())
    println(numbers.asSequence().take(3).toList()) // [1, 2, 3]
    val generated = generateSequence(5) { if (it < 25) it + 5 else null }
    println(generated.toList()) // [5, 10, 15, 20, 25]
    val infinite = generateSequence(0) { it + 1 }
    println(infinite.take(5).toList()) // [0, 1, 2, 3, 4]
    val built = sequence {
        yield(1)
        yieldAll(listOf(4, 7))
    }
    for (value in built) print("$value ")
    println()

    println("\n7. Ліниве виконання та порядок операцій")
    val pipeline = listOf(1, 2, 3, 4).asSequence()
        .filter { println("filter $it"); it % 2 == 0 }
        .map { println("map $it"); it * 10 }
        .take(1)
    println("До термінальної операції: filter/map ще не виконано")
    println(pipeline.toList()) // Перевіряє 1 і 2, map лише для 2; [20].
    println("Повторний обхід знову виконає обчислення")
    println(pipeline.toList())
    println("Ті самі операції над List виконуються одразу")
    val eager = listOf(1, 2, 3, 4)
        .filter { println("list filter $it"); it % 2 == 0 }
        .map { println("list map $it"); it * 10 }
        .take(1)
    println(eager) // [20], але filter перевірив усі 4 значення.

    println("\n8. Стан та одноразова послідовність")
    println(sequenceOf(3, 1, 3, 2).distinct().sorted().toList()) // [1, 2, 3]
    var counter = 0
    val oneShot = generateSequence { counter += 2; if (counter <= 8) counter else null }
    println(oneShot.toList()) // [2, 4, 6, 8]
    // oneShot.toList() // IllegalStateException: цей різновид обходиться лише раз.
}
