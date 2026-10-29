package ua.kpi.energyforecast.model

enum class AuthMode {
    LOGIN,
    REGISTER
}

enum class AuthField {
    NAME,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD
}

data class DemoUser(
    val id: String,
    val name: String,
    val email: String
)

/** Snapshot auth-форми: помилки полів, loading і результат операції оновлюють Compose UI. */
data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val errors: Map<AuthField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val user: DemoUser? = null
)