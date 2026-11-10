// Лекція 17. Навчальні приклади за розділами 8.4–8.7 PDF.
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

suspend fun sum(first: Int, second: Int): Int {
    delay(30L)
    return first + second
}

suspend fun resultsExample() = coroutineScope {
    val first: Deferred<Int> = async { sum(1, 2) }
    val second = async { sum(3, 4) }
    val third = async { sum(5, 6) }
    println("Результати: ${first.await()}, ${second.await()}, ${third.await()}") // 3, 7, 11

    val lazyResult = async(start = CoroutineStart.LAZY) { sum(10, 20) }
    println("LAZY до await: active=${lazyResult.isActive}")
    println("LAZY результат: ${lazyResult.await()}") // await запускає обчислення.

    val a = async(start = CoroutineStart.LAZY) { sum(1, 2) }
    val b = async(start = CoroutineStart.LAZY) { sum(3, 4) }
    a.start()
    b.start() // Обидва запущено до очікування результатів.
    println("Два LAZY-результати: ${a.await() + b.await()}") // 10
}

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
suspend fun dispatcherExamples() = coroutineScope {
    launch { println("Успадкований: ${Thread.currentThread().name}") }.join()
    launch(Dispatchers.Default) { println("Default: ${Thread.currentThread().name}") }.join()
    withContext(Dispatchers.IO) {
        // Лише демонстрація контексту; реальний блокувальний I/O розміщують тут.
        println("IO: ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Unconfined) {
        println("Unconfined до delay: ${Thread.currentThread().name}")
        delay(20L)
        println("Unconfined після delay: ${Thread.currentThread().name}")
    }.join()
    // Виділений потік із PDF обов'язково закриваємо після завершення роботи.
    newSingleThreadContext("LectureWorker").use { dispatcher ->
        withContext(dispatcher) { println("Власний: ${Thread.currentThread().name}") }
    }
}

suspend fun cancelLaunchExample() = coroutineScope {
    val ready = CompletableDeferred<Unit>()
    val downloader = launch {
        try {
            println("Завантаження почалося")
            ready.complete(Unit)
            awaitCancellation() // Детерміновано очікуємо скасування замість гонки таймерів.
        } catch (e: CancellationException) {
            println("Завантаження скасовано")
            throw e // Зберігаємо семантику скасування.
        } finally {
            println("finally: звільнення ресурсів")
        }
    }
    ready.await() // Дочірня корутина точно дійшла до своєї роботи.
    downloader.cancelAndJoin()
    println("Завершено: ${downloader.isCompleted}; скасовано: ${downloader.isCancelled}")
}

suspend fun cancelAsyncExample() = coroutineScope {
    val result = async { delay(1000L); "Це значення вже не потрібне" }
    result.cancelAndJoin()
    try {
        println(result.await())
    } catch (e: CancellationException) {
        println("await скасованого Deferred кидає CancellationException")
        // Це очікуване скасування окремої дитини; батько залишається активним.
        currentCoroutineContext().ensureActive()
    }
}

suspend fun cooperativeCpuExample() = coroutineScope {
    val started = CompletableDeferred<Unit>()
    val worker = launch(Dispatchers.Default) {
        started.complete(Unit)
        var steps = 0L
        try {
            while (true) {
                ensureActive() // CPU-цикл теж має співпрацювати зі скасуванням.
                steps++
            }
        } finally {
            println("CPU-роботу завершено; кількість кроків залежить від планувальника: $steps")
        }
    }
    started.await()
    worker.cancelAndJoin()
}

suspend fun channelExamples() = coroutineScope {
    val numbers = Channel<Int>() // Rendezvous: без буфера.
    val sender = launch {
        try {
            for (number in 1..3) numbers.send(number)
        } finally {
            numbers.close()
        }
    }
    repeat(3) { println("Отримано: ${numbers.receive()}") }
    sender.join()

    val users = Channel<String>(capacity = 2)
    launch {
        try {
            for (user in listOf("Taras", "Olena", "Stepan")) users.send(user)
        } finally {
            users.close()
        }
    }
    for (user in users) println("Канал із close: $user")
    println("for завершився після отримання всіх даних закритого каналу")
}

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.produceUsers(): ReceiveChannel<String> = produce {
    for (name in listOf("Taras", "Volodymyr", "Stepan")) send(name)
    // produce закриває канал автоматично при завершенні.
}

fun main() = runBlocking {
    println("1. async, Deferred, await та LAZY")
    resultsExample()
    println("\n2. Диспетчери; назви потоків можуть відрізнятися")
    dispatcherExamples()
    println("\n3. Скасування launch і finally")
    cancelLaunchExample()
    println("\n4. Скасування async")
    cancelAsyncExample()
    println("\n5. Кооперативне скасування CPU-циклу")
    cooperativeCpuExample()
    println("\n6. Channel: send/receive, close і for")
    channelExamples()
    println("\n7. Producer-consumer")
    produceUsers().consumeEach { println("produce: $it") }
}
