// Лекція 16. Навчальні приклади за розділами 8.1–8.3 PDF.
import kotlinx.coroutines.*

suspend fun doWork(label: String, count: Int = 3) {
    repeat(count) { index ->
        delay(30L)
        println("$label: ${index + 1}")
    }
}

suspend fun sequentialExample() {
    doWork("Послідовно", 2)
    println("Наступна інструкція — після doWork")
}

suspend fun concurrentExample() = coroutineScope {
    launch { doWork("Дочірня робота", 2) }
    println("Батьківський код продовжується")
    // coroutineScope повернеться лише після завершення дочірньої корутини.
}

suspend fun severalChildren() = coroutineScope {
    launch { doWork("A") }
    launch { doWork("B") }
    println("Обидві корутини створено")
    // Порядок A/B не є контрактом прикладу.
}

suspend fun nestedChildren() = coroutineScope {
    val outer: Job = launch {
        println("Зовнішня: початок")
        launch {
            delay(30L)
            println("Вкладена: завершення")
        }
        println("Тіло зовнішньої завершилось; вкладена ще може працювати")
    }
    outer.join() // Чекає також дочірню корутину outer.
    println("outer.join(): зовнішня та її дитина завершені")
}

suspend fun lazyJobs() = coroutineScope {
    val job = launch(start = CoroutineStart.LAZY) {
        println("LAZY: тіло запущено")
        delay(20L)
    }
    println("До start: active=${job.isActive}, completed=${job.isCompleted}")
    job.start()
    job.join()
    println("Після join: completed=${job.isCompleted}, cancelled=${job.isCancelled}")

    val startedByJoin = launch(start = CoroutineStart.LAZY) {
        println("LAZY також запускається через join()")
    }
    startedByJoin.join()

    val unused = launch(start = CoroutineStart.LAZY) {
        println("Цей рядок не виконається")
    }
    unused.cancel() // Незатребувана LAZY-дитина не повинна залишатися в scope.
    unused.join()
    println("Невикористану корутину скасовано: ${unused.isCancelled}")
}

fun main() = runBlocking {
    println("1. suspend не запускає окреме завдання автоматично")
    sequentialExample()

    println("\n2. launch та coroutineScope")
    concurrentExample()
    println("Повернулися з concurrentExample: дитина вже завершилася")

    println("\n3. Кілька дочірніх корутин")
    severalChildren()
    println("Усі роботи A/B завершені")

    println("\n4. Вкладені корутини й Job.join")
    nestedChildren()

    println("\n5. Явне очікування Job")
    val job: Job = launch { doWork("Job", 2) }
    println("Перед join")
    job.join()
    println("Після join: ${job.isCompleted}")

    println("\n6. Відкладений запуск")
    lazyJobs()

    println("\n7. Корутина не дорівнює окремому потоку")
    println("runBlocking: ${Thread.currentThread().name}")
    launch {
        println("Успадкований контекст, до delay: ${Thread.currentThread().name}")
        delay(20L)
        println("Успадкований контекст, після delay: ${Thread.currentThread().name}")
    }.join()
    println("У цьому консольному прикладі можна працювати конкурентно в одному потоці")
}
