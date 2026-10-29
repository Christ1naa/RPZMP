package ua.kpi.energyforecast.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Повторно використовуване поле: значення, помилка й доступність надходять від форми.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    hint: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        supportingText = {
            // Помилка має пріоритет над підказкою.
            val supportingMessage = error ?: hint
            if (supportingMessage != null) {
                Text(supportingMessage)
            }
        }
    )
}

/**
 * Відображає пароль і помилку форми; локально керує лише видимістю символів.
 * Видимість скидається після відтворення компонента, сам пароль належить AuthViewModel.
 */
@Composable
fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    enabled: Boolean,
    hint: String? = null
) {
    var passwordVisible by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(
                onClick = {
                    passwordVisible = !passwordVisible
                },
                enabled = enabled
            ) {
                Text(
                    if (passwordVisible) {
                        "Сховати"
                    } else {
                        "Показати"
                    }
                )
            }
        },
        supportingText = {
            // Помилка має пріоритет над підказкою.
            val supportingMessage = error ?: hint
            if (supportingMessage != null) {
                Text(supportingMessage)
            }
        }
    )
}