// Лекція 10. Навчальні приклади за розділами 5.1–5.3 PDF.

class Person<T>(val id: T, val name: String) {
    fun checkId(candidate: T): Boolean = id == candidate
}

class Word<K, V>(val source: K, var target: V)

fun <T> display(value: T) = println(value)

// Порівнюємо довжини масивів; за рівних довжин повертаємо другий.
fun <T> getBiggest(first: Array<T>, second: Array<T>): Array<T> =
    if (first.size > second.size) first else second

fun <T : Comparable<T>> maximum(first: T, second: T): T =
    if (first > second) first else second

fun <T> numericMaximum(first: T, second: T): T
    where T : Number, T : Comparable<T> =
    if (first > second) first else second

interface Message { val text: String }
interface Logger { fun log() }

class EmailMessage(override val text: String) : Message, Logger {
    override fun log() = println("Email: $text")
}

class SmsMessage(override val text: String) : Message, Logger {
    override fun log() = println("SMS: $text")
}

fun <T : Message> send(message: T) = println(message.text)

fun <T> sendLogged(message: T) where T : Message, T : Logger {
    println("Текст: ${message.text}")
    message.log()
}

class Messenger<T> where T : Message, T : Logger {
    fun send(message: T) = message.log()
}

// Без in/out параметр інваріантний: читати й записувати можна саме T.
class MessageBox<T : Message>(var value: T)

interface Producer<out T : Message> {
    fun create(text: String): T
}

class EmailProducer : Producer<EmailMessage> {
    override fun create(text: String) = EmailMessage(text)
}

interface Consumer<in T : Message> {
    fun accept(message: T)
}

class AnyMessageConsumer : Consumer<Message> {
    override fun accept(message: Message) = println("Прийнято: ${message.text}")
}

fun main() {
    println("1. Узагальнений клас і виведення типу")
    val taras: Person<Int> = Person(367, "Тарас")
    val olena = Person("A65", "Олена")
    println("${taras.id}: ${taras.name}")
    println("${olena.id}: ${olena.name}")
    println(olena.checkId("A65")) // true
    println(olena.checkId("B12")) // false
    // olena.checkId(65) // Помилка: для цього об'єкта потрібен String.

    println("\n2. Кілька параметрів типу")
    val translation = Word("one", "один")
    val number = Word("two", 2)
    println("${translation.source} — ${translation.target}")
    println("${number.source} — ${number.target}")

    println("\n3. Узагальнені функції")
    display("Hello Kotlin")
    display(1234)
    display(true)
    println(getBiggest(arrayOf(1, 2), arrayOf(3, 4, 5)).joinToString())
    println(getBiggest(arrayOf("Тарас", "Степан", "Олена"), arrayOf("Ірина")).joinToString())
    println(getBiggest(arrayOf(1), arrayOf(9)).joinToString()) // 9: рівні довжини.

    println("\n4. Обмеження типів")
    println(maximum(1, 2)) // 2
    println(maximum("A", "B")) // B
    println(numericMaximum(1.6, -2.8)) // 1.6
    // numericMaximum("A", "B") // String не є Number.
    val email = EmailMessage("Вивчаю generics")
    send(email)
    send<SmsMessage>(SmsMessage("Привіт"))
    sendLogged(email)
    val outlook = Messenger<EmailMessage>()
    outlook.send(email)
    val smsMessenger: Messenger<SmsMessage> = Messenger()
    smsMessenger.send(SmsMessage("Інший тип повідомлення"))

    println("\n5. Інваріантність")
    val emailBox = MessageBox(email)
    println(emailBox.value.text)
    // val messageBox: MessageBox<Message> = emailBox // Помилка типів.
    // Інакше в emailBox можна було б записати SmsMessage.

    println("\n6. Коваріантність: out — джерело значень")
    val emailProducer: Producer<EmailMessage> = EmailProducer()
    val messageProducer: Producer<Message> = emailProducer
    println(messageProducer.create("Створено через Producer<Message>").text)

    println("\n7. Контрваріантність: in — споживач значень")
    val anyConsumer: Consumer<Message> = AnyMessageConsumer()
    val emailConsumer: Consumer<EmailMessage> = anyConsumer
    emailConsumer.accept(email)
    anyConsumer.accept(SmsMessage("Споживач підтримує й SMS"))
}
