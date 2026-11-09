package ua.kpi.energyforecast.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EnergyApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): LoginResponseDto

    @GET("scenarios")
    suspend fun getScenarios(): ScenarioListDto

    @GET("scenarios/{id}")
    suspend fun getScenario(
        @Path("id") id: String
    ): ScenarioDto

    @GET("admin/summary")
    suspend fun getAdminSummary(): AdminSummaryDto
}