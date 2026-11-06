package ua.kpi.energyforecast.ui.navigation

import android.net.Uri
/**
 * Назви екранів і аргументів навігації.
 * Між екранами передаємо параметри, а не готові UI-компоненти.
 */
object AppRoutes {
    const val MENU = "menu"
    const val APPLIANCES = "appliances"
    const val DEMO = "demo"

    const val DAYS = "days"
    const val QUICK_FORECAST = "quick_forecast/{$DAYS}"

    const val HISTORY = "history"
    const val FORECAST_ID = "forecastId"
    const val FORECAST_DETAILS = "forecast_details/{$FORECAST_ID}"

    fun forecastDetails(id: String): String {
        require(id.isNotBlank())
        return "forecast_details/${Uri.encode(id)}"
    }

    fun quickForecast(days: Int): String {
        require(days in 1..365)
        return "quick_forecast/$days"
    }
}