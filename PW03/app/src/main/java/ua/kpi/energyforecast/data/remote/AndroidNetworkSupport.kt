package ua.kpi.energyforecast.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import java.io.File
import java.io.IOException
import java.time.Instant

class AndroidNetworkProbe(context: Context) {

    private val manager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    fun isAvailable(): Boolean {
        return manager?.activeNetwork != null
    }
}

/**
 * Пише лише службові характеристики запиту.
 * Паролі, токени, заголовки та тіла повідомлень не записує.
 */
class FileNetworkLogger(context: Context) {

    private val directory = File(
        context.applicationContext.filesDir,
        "logs"
    )

    @Synchronized
    fun write(entry: NetworkLogEntry) {
        try {
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Cannot create log directory")
            }

            val current = File(directory, "network.log")
            val previous = File(directory, "network.previous.log")

            // Залишаємо поточний журнал і одну попередню частину.
            if (current.exists() && current.length() >= 64 * 1024) {
                if (previous.exists() && !previous.delete()) {
                    throw IOException("Cannot remove previous log")
                }

                if (!current.renameTo(previous)) {
                    throw IOException("Cannot rotate network log")
                }
            }

            val line = buildString {
                append(Instant.now())
                append(" operation=")
                append(entry.operation.name)
                append(" attempt=")
                append(entry.attempt)
                append(" result=")
                append(entry.result)
                append(" http=")
                append(entry.httpCode ?: "-")
                append(" durationMs=")
                append(entry.durationMillis)
                append('\n')
            }

            current.appendText(line, Charsets.UTF_8)
        } catch (error: IOException) {
            // Збій журналу не перетворює успішний запит на невдалий.
            Log.w("EnergyNetwork", "Cannot write network log")
        }
    }
}