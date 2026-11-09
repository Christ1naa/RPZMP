package ua.kpi.energyforecast.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val EMULATOR_BASE_URL = "http://10.0.2.2:5156/"

    /**
     * Отримує актуальний токен перед кожним захищеним запитом.
     * Постійне сховище сесії підключимо на етапі автентифікації.
     */
    fun create(
        baseUrl: String,
        tokenProvider: () -> String? = { null }
    ): EnergyApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            // Контрольовані повтори додамо окремою політикою.
            .retryOnConnectionFailure(false)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                val isLogin = original.url.encodedPath == "/auth/login"

                if (!isLogin) {
                    val token = tokenProvider()

                    if (!token.isNullOrBlank()) {
                        builder.header("Authorization", "Bearer $token")
                    }
                }

                chain.proceed(builder.build())
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