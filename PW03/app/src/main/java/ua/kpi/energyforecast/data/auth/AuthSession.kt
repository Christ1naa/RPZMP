package ua.kpi.energyforecast.data.auth

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.UUID
import ua.kpi.energyforecast.data.remote.ApiJson
import ua.kpi.energyforecast.model.DemoUser

enum class UserRole {
    USER,
    ADMIN
}

data class AuthSession(
    val accessToken: String,
    val user: DemoUser,
    val role: UserRole,
    val expiresAtSeconds: Long,
    val sessionKey: String = UUID.randomUUID().toString()
) {
    // Не допускаємо випадкового виведення токена через toString().
    override fun toString(): String {
        return "AuthSession(userId=${user.id}, role=$role)"
    }
}

data class ApiSessionState(
    val isReady: Boolean = false,
    val session: AuthSession? = null,
    val message: String? = null
)

/**
 * Читає дані JWT для UI та перевіряє локальний строк дії.
 * Це не криптографічна перевірка підпису: її виконує сервер.
 */
object JwtSessionParser {

    private data class Header(
        val alg: String? = null
    )

    private data class Payload(
        val sub: String? = null,
        val email: String? = null,
        val name: String? = null,
        val role: String? = null,
        val iat: Long? = null,
        val exp: Long? = null
    )

    fun parse(
        token: String,
        nowSeconds: Long
    ): AuthSession {
        require(token.isNotBlank() && token.length <= 16_384) {
            "Некоректний токен."
        }

        val parts = token.split('.')

        require(parts.size == 3 && parts.all { it.isNotBlank() }) {
            "Некоректний формат токена."
        }

        val decoder = Base64.getUrlDecoder()
        val gson = ApiJson.create()

        val header = requireNotNull(
            gson.fromJson(
                String(decoder.decode(parts[0]), StandardCharsets.UTF_8),
                Header::class.java
            )
        )

        require(header.alg == "HS256") {
            "Непідтримуваний формат токена."
        }

        // Перевіряємо структуру підпису HS256, але не його достовірність.
        require(decoder.decode(parts[2]).size == 32) {
            "Некоректна структура підпису."
        }

        val payload = requireNotNull(
            gson.fromJson(
                String(decoder.decode(parts[1]), StandardCharsets.UTF_8),
                Payload::class.java
            )
        )

        val id = payload.sub.requiredText()
        val email = payload.email.requiredText()
            .lowercase(Locale.ROOT)
        val name = payload.name.requiredText()

        require(
            email.length <= 254 &&
                    Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
        ) {
            "Токен містить некоректний email."
        }

        val role = UserRole.entries.firstOrNull {
            it.name == payload.role
        } ?: throw IllegalArgumentException("Невідома роль у токені.")

        val issuedAt = requireNotNull(payload.iat)
        val expiresAt = requireNotNull(payload.exp)

        require(issuedAt >= 0 && expiresAt > issuedAt) {
            "Некоректний час сесії."
        }

        require(expiresAt > nowSeconds) {
            "Сесію завершено. Увійдіть повторно."
        }

        return AuthSession(
            accessToken = token,
            user = DemoUser(
                id = id,
                name = name,
                email = email
            ),
            role = role,
            expiresAtSeconds = expiresAt
        )
    }

    private fun String?.requiredText(): String {
        return this?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException(
                "Токен не містить обов'язкових даних."
            )
    }
}