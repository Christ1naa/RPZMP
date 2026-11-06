package ua.kpi.energyforecast.data.remote

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    val accessToken: String? = null,
    val tokenType: String? = null,
    val expiresAt: Long? = null
)

data class ScenarioListDto(
    val scenarios: List<ScenarioDto?>? = null
)

data class ScenarioDto(
    val id: String? = null,
    val name: String? = null,
    val category: String? = null,
    val description: String? = null,
    val devices: List<ScenarioDeviceDto?>? = null
)

data class ScenarioDeviceDto(
    val id: String? = null,
    val name: String? = null,
    val powerWatts: Double? = null,
    val hoursPerDay: Double? = null,
    val deviceCount: Int? = null
)

data class AdminSummaryDto(
    val scenarioCount: Int? = null,
    val devicePositionCount: Int? = null
)