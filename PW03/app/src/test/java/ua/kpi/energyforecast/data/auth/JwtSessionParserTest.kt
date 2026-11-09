package ua.kpi.energyforecast.data.auth

import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class JwtSessionParserTest {

    private val now = 1000L

    /**
     * Структурний JWT без справжнього серверного підпису.
     * Ці тести перевіряють читання полів, а не автентичність.
     */
    private fun token(
        role: String = "USER",
        expiresAt: Long = 2000,
        email: String = "demo@energy.test"
    ): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()

        fun encode(value: String): String {
            return encoder.encodeToString(
                value.toByteArray(StandardCharsets.UTF_8)
            )
        }

        val header = encode("""{"alg":"HS256","typ":"JWT"}""")

        val payload = encode(
            """
            {
              "sub":"demo-user",
              "email":"$email",
              "name":"Демо користувач",
              "role":"$role",
              "iat":900,
              "exp":$expiresAt
            }
            """.trimIndent()
        )

        val signature = encoder.encodeToString(ByteArray(32))

        return "$header.$payload.$signature"
    }

    @Test
    fun readsUserAndExpiration() {
        val session = JwtSessionParser.parse(token(), now)

        assertEquals("demo-user", session.user.id)
        assertEquals("Демо користувач", session.user.name)
        assertEquals("demo@energy.test", session.user.email)
        assertEquals(UserRole.USER, session.role)
        assertEquals(2000L, session.expiresAtSeconds)
    }

    @Test
    fun readsAdminRole() {
        val session = JwtSessionParser.parse(
            token(role = "ADMIN"),
            now
        )

        assertEquals(UserRole.ADMIN, session.role)
    }

    @Test
    fun rejectsExpiredToken() {
        assertThrows(IllegalArgumentException::class.java) {
            JwtSessionParser.parse(
                token(expiresAt = now),
                now
            )
        }
    }

    @Test
    fun rejectsUnknownRole() {
        assertThrows(IllegalArgumentException::class.java) {
            JwtSessionParser.parse(
                token(role = "OWNER"),
                now
            )
        }
    }

    @Test
    fun rejectsMalformedToken() {
        assertThrows(IllegalArgumentException::class.java) {
            JwtSessionParser.parse("broken-token", now)
        }
    }

    @Test
    fun sessionDescriptionDoesNotExposeToken() {
        val rawToken = token()
        val session = JwtSessionParser.parse(rawToken, now)

        assertFalse(session.toString().contains(rawToken))
    }
}