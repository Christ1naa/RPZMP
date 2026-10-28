# Лекція 1 — Вступ до Kotlin

## Основне
- Kotlin — статично типізована мова від JetBrains.
- `fun` — оголошення функції.
- `main()` — точка входу в консольну програму.
- `println()` — виведення значення з переходом на новий рядок після нього.
- `.kt` — вихідний код; `.jar` — результат компіляції.
- `kotlinc` компілює код, JVM виконує його.

## Контрольні запитання
1. Kotlin — мова програмування від JetBrains.
2. Платформи: Windows, Linux, macOS, Android, iOS; також веб.
3. Kotlin 1.0 — 15 лютого 2016 року.
4. Вплив: Java, Scala, Groovy, C#, JavaScript, Swift.
5. Для Android: сумісність із Java, лаконічність, безпечніша робота з null.
6. Оголошення функції — `fun`.
7. Точка входу — `main()`.
8. Виведення — `println()`.
9. Розширення — `.kt`.
10. Скомпілювати через `kotlinc`, запустити через `java -jar`.

## Запуск із папки lecture-01
```cmd
"C:\Tools\kotlinc\kotlinc\bin\kotlinc.bat" tasks.kt -include-runtime -d tasks.jar
java -jar tasks.jar
```
Після зміни коду зберегти файл і повторити компіляцію.

## Виконано
- Завдання 1–3 — у `tasks.kt`.
- Завдання 4 — компіляція та запуск через CMD.
- Завдання 5 — `HelloKotlin/src/app.kt`, запуск в IntelliJ успішний.