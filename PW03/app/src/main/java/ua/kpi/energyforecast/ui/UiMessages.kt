
package ua.kpi.energyforecast.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Короткі повідомлення для активного інтерфейсу. */
object UiMessages {

    private val mutableMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 8
    )

    val messages = mutableMessages.asSharedFlow()

    fun show(message: String) {
        mutableMessages.tryEmit(message)
    }
}