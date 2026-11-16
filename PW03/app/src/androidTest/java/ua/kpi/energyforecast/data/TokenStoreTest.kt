package ua.kpi.energyforecast.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.data.auth.PreferencesTokenStore

class TokenStoreTest {
    @Test fun tokenSurvivesReopeningAndLogoutRemovesIt() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "session-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun open() = PreferencesTokenStore(PreferenceDataStoreFactory.create(
            scope = scope, produceFile = { file }))
        try {
            var store = open()
            assertNull(store.read())
            store.write("test-session-token")
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            store = open()
            assertEquals("test-session-token", store.read())
            store.clear()
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            assertNull(open().read())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
