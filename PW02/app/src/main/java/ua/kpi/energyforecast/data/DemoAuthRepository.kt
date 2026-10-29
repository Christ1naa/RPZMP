package ua.kpi.energyforecast.data

import ua.kpi.energyforecast.model.DemoUser
import java.util.Locale
import java.util.UUID

sealed interface AuthResult {

    data class Success(
        val user: DemoUser
    ) : AuthResult

    data class Failure(
        val message: String
    ) : AuthResult
}

/**
 * Навчальне сховище в пам’яті одного AuthViewModel: без сервера та постійного збереження.
 * Поворот зберігає ViewModel, process death — ні; новий екземпляр містить лише демоакаунт.
 * Паролі зберігаються відкрито лише для демонстрації, тому слід вводити вигадані дані.
 * Методи викликаються послідовно з AuthViewModel після перевірки форми.
 */
class DemoAuthRepository {

    private data class Account(
        val user: DemoUser,
        val password: String
    )

    private val accounts = mutableMapOf(
        DEMO_EMAIL to Account(
            user = DemoUser(
                id = "demo-user",
                name = "Демо користувач",
                email = DEMO_EMAIL
            ),
            password = DEMO_PASSWORD
        )
    )

    /** Шукає нормалізований email, але порівнює пароль без trim та зміни регістру. */
    fun login(
        email: String,
        password: String
    ): AuthResult {
        val account = accounts[normalizeEmail(email)]

        return if (
            account != null &&
            account.password == password
        ) {
            AuthResult.Success(account.user)
        } else {
            AuthResult.Failure("Неправильний email або пароль")
        }
    }

    /** Відхиляє дублі email; новому акаунту призначає стабільний у цьому сеансі UUID. */
    fun register(
        name: String,
        email: String,
        password: String
    ): AuthResult {
        val normalizedEmail = normalizeEmail(email)

        if (accounts.containsKey(normalizedEmail)) {
            return AuthResult.Failure(
                "Акаунт із таким email уже існує"
            )
        }

        val user = DemoUser(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            email = normalizedEmail
        )

        accounts[normalizedEmail] = Account(
            user = user,
            password = password
        )

        return AuthResult.Success(user)
    }

    // Єдиний ключ для входу й реєстрації; Locale.ROOT усуває залежність від мови пристрою.
    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    companion object {
        const val DEMO_EMAIL = "demo@energy.test"
        const val DEMO_PASSWORD = "Demo1234"
    }
}