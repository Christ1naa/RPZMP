package ua.kpi.energyforecast.logic

import ua.kpi.energyforecast.model.AuthField

/** Локальні правила навчальних форм; перевірку наявності акаунта виконує repository. */
object AuthValidator {

    private val emailPattern =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    /** Для входу перевіряє формат email і непорожній пароль, не змінюючи його символів. */
    fun validateLogin(
        email: String,
        password: String
    ): Map<AuthField, String> {
        val errors = mutableMapOf<AuthField, String>()

        validateEmail(email)?.let {
            errors[AuthField.EMAIL] = it
        }

        if (password.isBlank()) {
            errors[AuthField.PASSWORD] = "Введіть пароль"
        }

        return errors
    }

    /**
     * Ім’я після trim: 2–60 символів; пароль: 8–64, хоча б одна літера й цифра.
     * Підтвердження має бути непорожнім і точно збігатися; повертаються помилки всіх полів.
     */
    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Map<AuthField, String> {
        val errors = mutableMapOf<AuthField, String>()

        if (name.trim().length !in 2..60) {
            errors[AuthField.NAME] =
                "Ім’я має містити від 2 до 60 символів"
        }

        validateEmail(email)?.let {
            errors[AuthField.EMAIL] = it
        }

        when {
            password.length !in 8..64 -> {
                errors[AuthField.PASSWORD] =
                    "Пароль має містити від 8 до 64 символів"
            }

            !password.any { it.isLetter() } ||
                    !password.any { it.isDigit() } -> {
                errors[AuthField.PASSWORD] =
                    "Додайте хоча б одну літеру та одну цифру"
            }
        }

        when {
            confirmPassword.isEmpty() -> {
                errors[AuthField.CONFIRM_PASSWORD] =
                    "Повторіть пароль"
            }

            password != confirmPassword -> {
                errors[AuthField.CONFIRM_PASSWORD] =
                    "Паролі не збігаються"
            }
        }

        return errors
    }

    // Навчальна перевірка: після trim до 254 символів, без пробілів, із @ та крапкою в домені.
    private fun validateEmail(email: String): String? {
        val normalized = email.trim()

        return when {
            normalized.isEmpty() -> "Введіть email"

            normalized.length > 254 ||
                    !emailPattern.matches(normalized) -> {
                "Введіть коректний email"
            }

            else -> null
        }
    }
}