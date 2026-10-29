package ua.kpi.energyforecast.ui.navigation

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

    fun quickForecast(days: Int): String {
        require(days in 1..365)
        return "quick_forecast/$days"
    }
}