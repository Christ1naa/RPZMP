package ua.kpi.energyforecast.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.Strictness

/**
 * Перевіряє JSON-типи до перетворення на DTO.
 * Наприклад, рядок "60" не приймається замість числа 60.
 */
object ApiJson {

    fun create(): Gson {
        val stringAdapter = JsonDeserializer<String> { json, _, _ ->
            if (!json.isJsonPrimitive || !json.asJsonPrimitive.isString) {
                throw JsonParseException("Очікується JSON-рядок")
            }

            json.asString
        }

        val doubleAdapter = JsonDeserializer<Double> { json, _, _ ->
            requireNumber(json)

            val value = json.asString.toDoubleOrNull()
                ?: throw JsonParseException("Некоректне число")

            if (!value.isFinite()) {
                throw JsonParseException("Число має бути скінченним")
            }

            value
        }

        val intAdapter = JsonDeserializer<Int> { json, _, _ ->
            requireNumber(json)

            try {
                json.asString.toBigDecimal().intValueExact()
            } catch (exception: NumberFormatException) {
                throw JsonParseException("Некоректне ціле число", exception)
            } catch (exception: ArithmeticException) {
                throw JsonParseException(
                    "Очікується ціле число в межах Int",
                    exception
                )
            }
        }

        val longAdapter = JsonDeserializer<Long> { json, _, _ ->
            requireNumber(json)

            try {
                json.asString.toBigDecimal().longValueExact()
            } catch (exception: NumberFormatException) {
                throw JsonParseException("Некоректний час", exception)
            } catch (exception: ArithmeticException) {
                throw JsonParseException(
                    "Очікується ціле число в межах Long",
                    exception
                )
            }
        }

        return GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .registerTypeAdapter(String::class.java, stringAdapter)
            .registerTypeAdapter(Double::class.javaObjectType, doubleAdapter)
            .registerTypeAdapter(Int::class.javaObjectType, intAdapter)
            .registerTypeAdapter(Long::class.javaObjectType, longAdapter)
            .create()
    }

    private fun requireNumber(json: JsonElement) {
        if (!json.isJsonPrimitive || !json.asJsonPrimitive.isNumber) {
            throw JsonParseException("Очікується JSON-число")
        }
    }
}