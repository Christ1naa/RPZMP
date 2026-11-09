package ua.kpi.energyforecast.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * На Android 17 запитує дозвіл перед локальним мережевим запитом.
 * На попередніх версіях одразу виконує дію.
 */
@Composable
fun rememberLocalNetworkAction(
    onAllowed: () -> Unit,
    onDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentOnAllowed by rememberUpdatedState(onAllowed)
    val currentOnDenied by rememberUpdatedState(onDenied)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            currentOnAllowed()
        } else {
            currentOnDenied()
        }
    }

    return {
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"

        if (
            Build.VERSION.SDK_INT >= 37 &&
            context.checkSelfPermission(permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(permission)
        } else {
            currentOnAllowed()
        }
    }
}