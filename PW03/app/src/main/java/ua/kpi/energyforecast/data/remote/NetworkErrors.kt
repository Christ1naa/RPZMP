package ua.kpi.energyforecast.data.remote

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

class NoNetworkException : IOException("No active network")

object NetworkErrors {

    fun message(
        error: Exception,
        isLogin: Boolean = false
    ): String {
        return when (error) {
            is NoNetworkException ->
                "Немає активного мережевого підключення."

            is SocketTimeoutException ->
                "Сервер не відповів вчасно. Спробуйте ще раз."

            is HttpException -> when (error.code()) {
                400, 422 ->
                    "Перевірте дані запиту."

                401 -> if (isLogin) {
                    "Неправильний email або пароль."
                } else {
                    "Сесію завершено. Увійдіть повторно."
                }

                403 ->
                    "Недостатньо прав для цієї дії."

                404 ->
                    "Запитаний ресурс не знайдено."

                408 ->
                    "Сервер не встиг обробити запит."

                429 ->
                    "Забагато запитів. Спробуйте пізніше."

                in 500..599 ->
                    "Сервер тимчасово недоступний. Спробуйте пізніше."

                else ->
                    "Не вдалося виконати запит."
            }

            is InvalidApiDataException,
            is JsonParseException,
            is MalformedJsonException,
            is EOFException ->
                "Сервер повернув некоректні дані."

            is IOException ->
                "Не вдалося підключитися до сервера. " +
                        "Перевірте мережу та запуск сервера."

            else -> if (isLogin) {
                "Не вдалося відкрити або зберегти сесію."
            } else {
                "Не вдалося виконати операцію."
            }
        }
    }
}