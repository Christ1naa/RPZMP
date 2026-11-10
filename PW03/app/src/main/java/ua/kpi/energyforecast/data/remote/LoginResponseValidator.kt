package ua.kpi.energyforecast.data.remote

import com.google.gson.JsonParseException
import java.util.Locale
import ua.kpi.energyforecast.data.auth.AuthSession
import ua.kpi.energyforecast.data.auth.JwtSessionParser

/** Перевіряє відповідь входу до журналювання успіху та збереження. */
object LoginResponseValidator {

    fun validate(
        response: LoginResponseDto,
        expectedEmail: String,
        nowSeconds: Long
    ): AuthSession {
        if (response.tokenType != "Bearer") {
            throw InvalidApiDataException(
                "Сервер повернув непідтримуваний тип токена."
            )
        }

        val token = response.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: throw InvalidApiDataException(
                "Сервер не повернув токен."
            )

        val session = try {
            JwtSessionParser.parse(token, nowSeconds)
        } catch (error: IllegalArgumentException) {
            throw InvalidApiDataException(
                "Сервер повернув некоректну або прострочену сесію."
            )
        } catch (error: JsonParseException) {
            throw InvalidApiDataException(
                "Сервер повернув некоректні дані сесії."
            )
        }

        if (response.expiresAt != session.expiresAtSeconds) {
            throw InvalidApiDataException(
                "Сервер повернув неузгоджений строк дії."
            )
        }

        val normalizedEmail = expectedEmail
            .trim()
            .lowercase(Locale.ROOT)

        if (session.user.email != normalizedEmail) {
            throw InvalidApiDataException(
                "Сервер повернув сесію іншого користувача."
            )
        }

        return session
    }
}