package ua.kpi.energyforecast.data.auth

import android.content.Context
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.kpi.energyforecast.data.remote.ApiClient
import ua.kpi.energyforecast.data.remote.LoginRequestDto

/**
 * Єдиний власник серверної сесії в процесі.
 * Відновлює токен, зберігає вхід і закриває завершені сесії.
 */
class ApiSessionRepository(
    private val store: TokenStore,
    private val scope: CoroutineScope,
    baseUrl: String = ApiClient.EMULATOR_BASE_URL,
    private val clockSeconds: () -> Long = {
        System.currentTimeMillis() / 1000
    }
) {

    private val mutableState = MutableStateFlow(ApiSessionState())
    val state = mutableState.asStateFlow()

    private val mutex = Mutex()
    private var expiryJob: Job? = null
    private var revision = 0L

    val api = ApiClient.create(
        baseUrl = baseUrl,
        tokenProvider = { currentToken() },
        onUnauthorized = { token ->
            invalidate(
                token,
                "Сесію завершено. Увійдіть повторно."
            )
        }
    )

    init {
        scope.launch {
            restore()
        }
    }

    private suspend fun restore() {
        mutex.withLock {
            try {
                val token = store.read()

                val session = if (token == null) {
                    null
                } else {
                    try {
                        JwtSessionParser.parse(
                            token,
                            clockSeconds()
                        )
                    } catch (exception: Exception) {
                        store.clear()
                        null
                    }
                }

                mutableState.value = ApiSessionState(
                    isReady = true,
                    session = session,
                    message = if (token != null && session == null) {
                        "Збережена сесія недійсна. Увійдіть повторно."
                    } else {
                        null
                    }
                )

                session?.let { scheduleExpiry(it) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableState.value = ApiSessionState(
                    isReady = true,
                    message = "Не вдалося прочитати збережену сесію."
                )
            }
        }
    }

    suspend fun login(email: String, password: String) {
        val startRevision = mutex.withLock {
            check(mutableState.value.isReady) {
                "Зачекайте завершення читання сесії."
            }
            revision
        }

        val normalizedEmail = email.trim().lowercase(Locale.ROOT)

        // Мережевий запит не утримує mutex сховища.
        val response = api.login(
            LoginRequestDto(
                email = normalizedEmail,
                password = password
            )
        )

        require(response.tokenType == "Bearer") {
            "Сервер повернув непідтримуваний тип токена."
        }

        val session = JwtSessionParser.parse(
            token = requireNotNull(response.accessToken),
            nowSeconds = clockSeconds()
        )

        require(response.expiresAt == session.expiresAtSeconds) {
            "Сервер повернув неузгоджений строк дії."
        }

        require(session.user.email == normalizedEmail) {
            "Сервер повернув сесію іншого користувача."
        }

        mutex.withLock {
            check(revision == startRevision) {
                "Стан сесії змінився. Повторіть вхід."
            }

            // Показуємо успіх лише після успішного запису в DataStore.
            store.write(session.accessToken)

            revision++
            mutableState.value = ApiSessionState(
                isReady = true,
                session = session
            )

            scheduleExpiry(session)
        }
    }

    suspend fun logout() {
        mutex.withLock {
            store.clear()

            revision++
            expiryJob?.cancel()
            expiryJob = null

            mutableState.value = ApiSessionState(isReady = true)
        }
    }

    /**
     * Викликається з потоку OkHttp без блокувального читання DataStore.
     * Прострочений токен не передається в новий запит.
     */
    private fun currentToken(): String? {
        val session = mutableState.value.session ?: return null

        if (session.expiresAtSeconds <= clockSeconds()) {
            invalidate(
                session.accessToken,
                "Сесію завершено. Увійдіть повторно."
            )
            return null
        }

        return session.accessToken
    }

    fun checkExpiry() {
        val session = mutableState.value.session ?: return

        if (session.expiresAtSeconds <= clockSeconds()) {
            invalidate(
                session.accessToken,
                "Сесію завершено. Увійдіть повторно."
            )
        }
    }

    private fun scheduleExpiry(session: AuthSession) {
        expiryJob?.cancel()

        expiryJob = scope.launch {
            while (
                mutableState.value.session?.sessionKey ==
                session.sessionKey
            ) {
                val remaining = session.expiresAtSeconds - clockSeconds()

                if (remaining <= 0) {
                    invalidate(
                        session.accessToken,
                        "Сесію завершено. Увійдіть повторно."
                    )
                    break
                }

                delay(minOf(remaining, 30L) * 1000)
            }
        }
    }

    /**
     * Відповідь старого запиту не повинна закривати іншу сесію.
     * Callback OkHttp лише планує асинхронне очищення.
     */
    private fun invalidate(token: String?, message: String) {
        if (token == null) return

        scope.launch {
            mutex.withLock {
                if (mutableState.value.session?.accessToken != token) {
                    return@withLock
                }

                revision++
                expiryJob?.cancel()
                expiryJob = null

                mutableState.value = ApiSessionState(
                    isReady = true,
                    message = message
                )

                try {
                    store.clear()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    mutableState.value = ApiSessionState(
                        isReady = true,
                        message = "$message Не вдалося очистити сховище."
                    )
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ApiSessionRepository? = null

        fun getInstance(context: Context): ApiSessionRepository {
            return instance ?: synchronized(this) {
                instance ?: ApiSessionRepository(
                    store = PreferencesTokenStore(
                        context.applicationContext
                    ),
                    scope = CoroutineScope(
                        SupervisorJob() + Dispatchers.Default
                    )
                ).also {
                    instance = it
                }
            }
        }
    }
}