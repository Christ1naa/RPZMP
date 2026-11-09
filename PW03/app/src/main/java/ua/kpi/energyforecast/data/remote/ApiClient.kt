package ua.kpi.energyforecast.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val EMULATOR_BASE_URL = "http://10.0.2.2:5156/"

    fun create(
        baseUrl: String,
        tokenProvider: () -> String? = { null },
        onUnauthorized: (String?) -> Unit = {}
    ): EnergyApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                val isLogin = original.url.encodedPath == "/auth/login"

                val requestToken = if (isLogin) {
                    null
                } else {
                    tokenProvider()
                }

                if (!requestToken.isNullOrBlank()) {
                    builder.header(
                        "Authorization",
                        "Bearer $requestToken"
                    )
                }

                val response = chain.proceed(builder.build())

                // Неправильний пароль не прирівнюємо
                // до відмови захищеного запиту.
                if (!isLogin && response.code == 401) {
                    onUnauthorized(requestToken)
                }

                response
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create(ApiJson.create())
            )
            .build()
            .create(EnergyApi::class.java)
    }
}