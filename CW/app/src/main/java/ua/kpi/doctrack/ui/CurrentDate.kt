package ua.kpi.doctrack.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import kotlinx.coroutines.delay

/** Shared by all date-sensitive screens; polling stops while the app is in the background. */
@Composable
internal fun rememberCurrentDate(): State<LocalDate> {
    val today = remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        today.value = LocalDate.now()
    }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                today.value = LocalDate.now()
                delay(60_000L)
            }
        }
    }
    return today
}
