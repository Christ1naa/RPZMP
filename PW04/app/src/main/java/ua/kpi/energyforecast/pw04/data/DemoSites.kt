package ua.kpi.energyforecast.pw04.data

import ua.kpi.energyforecast.pw04.model.*
import java.time.Instant

/** Усі назви, характеристики та прив'язки вигадані, це не реальна інфраструктура. */
object DemoSites {
    val sites = listOf(
        EnergySite("home-1", "Будинок Дніпро", SiteType.HOME, "Умовний житловий споживач з вечірнім навантаженням.", Coordinates(50.4501, 30.5234), 12.0, 8.0, SiteStatus.ACTIVE, Instant.parse("2026-11-01T08:00:00Z")),
        EnergySite("office-1", "Офіс Обрій", SiteType.OFFICE, "Умовний офіс: освітлення та робочі місця.", Coordinates(50.4547, 30.5138), 45.0, 10.0, SiteStatus.ACTIVE, Instant.parse("2026-11-03T09:00:00Z")),
        EnergySite("factory-1", "Майстерня Енергія", SiteType.INDUSTRY, "Умовна майстерня з обладнанням у дві зміни.", Coordinates(50.4395, 30.5310), 120.0, 16.0, SiteStatus.ACTIVE, Instant.parse("2026-11-02T10:00:00Z")),
        EnergySite("home-2", "Будинок Сад", SiteType.HOME, "Умовна будівля на обслуговуванні; нульове поточне навантаження.", Coordinates(50.4620, 30.5350), 18.0, 6.0, SiteStatus.MAINTENANCE, Instant.parse("2026-11-04T08:00:00Z")),
        EnergySite("office-2", "Офіс Липа", SiteType.OFFICE, "Умовний офіс з коротким робочим днем.", Coordinates(50.4450, 30.5080), 28.0, 7.0, SiteStatus.ACTIVE, Instant.parse("2026-11-05T08:00:00Z")),
        EnergySite("factory-2", "Цех Ритм", SiteType.INDUSTRY, "Умовний цех із плановим обслуговуванням.", Coordinates(50.4680, 30.5500), 200.0, 12.0, SiteStatus.MAINTENANCE, Instant.parse("2026-11-06T08:00:00Z"))
    )
}
