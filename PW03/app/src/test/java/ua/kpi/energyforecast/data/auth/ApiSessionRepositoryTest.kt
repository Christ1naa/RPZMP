package ua.kpi.energyforecast.data.auth

import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import ua.kpi.energyforecast.data.remote.ApiJson
import ua.kpi.energyforecast.data.remote.LoginTestFixtures

@OptIn(ExperimentalCoroutinesApi::class)
class ApiSessionRepositoryTest {
    private class MemoryStore(var token: String? = null) : TokenStore {
        var failWrite = false
        var failRead = false
        var cancelClear = false
        override suspend fun read(): String? {
            if (failRead) throw IOException("storage unavailable")
            return token
        }
        override suspend fun write(token: String) {
            if (failWrite) throw IOException("disk full")
            this.token = token
        }
        override suspend fun clear() {
            if (cancelClear) throw CancellationException("cancelled")
            token = null
        }
    }

    private val fixture = LoginTestFixtures.response()

    @Test fun restoresValidSessionWithoutNetworkAndLogoutPersists() = runTest {
        val store = MemoryStore(fixture.accessToken)
        val repository = ApiSessionRepository(store, backgroundScope,
            networkAvailable = { error("Restoration must stay offline") },
            clockSeconds = { LoginTestFixtures.NOW })
        assertFalse(repository.state.value.isReady)
        runCurrent()
        assertEquals(UserRole.USER, repository.state.value.session!!.role)
        repository.logout()
        assertNull(repository.state.value.session)
        assertNull(store.token)
        val restarted = ApiSessionRepository(store, backgroundScope,
            clockSeconds = { LoginTestFixtures.NOW })
        runCurrent()
        assertTrue(restarted.state.value.isReady)
        assertNull(restarted.state.value.session)
    }

    @Test fun invalidOrExpiredSavedTokensAreCleared() = runTest {
        for (token in listOf("broken", LoginTestFixtures.response(
            expiresAt = LoginTestFixtures.NOW).accessToken)) {
            val store = MemoryStore(token)
            val repository = ApiSessionRepository(store, backgroundScope,
                clockSeconds = { LoginTestFixtures.NOW })
            runCurrent()
            assertTrue(repository.state.value.isReady)
            assertNull(repository.state.value.session)
            assertNull(store.token)
            assertNotNull(repository.state.value.message)
        }
    }

    @Test fun readFailureDoesNotExposeProtectedSession() = runTest {
        val store = MemoryStore(fixture.accessToken).apply { failRead = true }
        val repository = ApiSessionRepository(store, backgroundScope)
        runCurrent()
        assertTrue(repository.state.value.isReady)
        assertNull(repository.state.value.session)
        assertNotNull(repository.state.value.message)
    }

    @Test fun cancellationDuringInvalidTokenCleanupIsNotConvertedToReadyState() = runTest {
        val store = MemoryStore("broken").apply { cancelClear = true }
        val repository = ApiSessionRepository(store, backgroundScope)
        runCurrent()
        assertFalse(repository.state.value.isReady)
        assertNull(repository.state.value.session)
    }

    @Test fun scheduledExpiryClearsSessionAndStorage() = runTest {
        val shortSession = LoginTestFixtures.response(expiresAt = LoginTestFixtures.NOW + 2)
        val store = MemoryStore(shortSession.accessToken)
        val repository = ApiSessionRepository(store, backgroundScope,
            clockSeconds = { LoginTestFixtures.NOW + testScheduler.currentTime / 1000 })
        runCurrent()
        assertNotNull(repository.state.value.session)
        advanceTimeBy(2000)
        runCurrent()
        assertNull(repository.state.value.session)
        assertNull(store.token)
        assertNotNull(repository.state.value.message)
    }

    @Test fun resumeCheckExpiresSessionAfterClockChange() = runTest {
        var now = LoginTestFixtures.NOW
        val store = MemoryStore(fixture.accessToken)
        val repository = ApiSessionRepository(store, backgroundScope, clockSeconds = { now })
        runCurrent()
        now = LoginTestFixtures.EXPIRES_AT
        repository.checkExpiry()
        runCurrent()
        assertNull(repository.state.value.session)
        assertNull(store.token)
    }

    @Test fun successfulLoginPersistsTokenAndNormalizesEmail() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(ApiJson.create().toJson(fixture)))
            val store = MemoryStore()
            val repository = ApiSessionRepository(store, backgroundScope,
                baseUrl = server.url("/").toString(), clockSeconds = { LoginTestFixtures.NOW })
            runCurrent()
            // Real HTTP finishes without advancing the virtual expiry clock.
            runBlocking { repository.login("  DEMO@ENERGY.TEST  ", "Demo1234") }
            assertEquals(fixture.accessToken, store.token)
            assertEquals(store.token, repository.state.value.session!!.accessToken)
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertNull(request.getHeader("Authorization"))
            assertTrue(request.body.readUtf8().contains("demo@energy.test"))
        } finally { server.shutdown() }
    }

    @Test fun failedTokenWriteNeverOpensProtectedSession() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(ApiJson.create().toJson(fixture)))
            val store = MemoryStore().apply { failWrite = true }
            val repository = ApiSessionRepository(store, backgroundScope,
                baseUrl = server.url("/").toString(), clockSeconds = { LoginTestFixtures.NOW })
            runCurrent()
            assertThrows(IOException::class.java) {
                runBlocking { repository.login("demo@energy.test", "Demo1234") }
            }
            assertNull(repository.state.value.session)
            assertNull(store.token)
        } finally { server.shutdown() }
    }

    @Test fun protected401ClearsSessionBut403PreservesIt() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val store = MemoryStore(fixture.accessToken)
            val repository = ApiSessionRepository(store, backgroundScope,
                baseUrl = server.url("/").toString(), clockSeconds = { LoginTestFixtures.NOW })
            runCurrent()
            for (code in listOf(403, 401)) {
                server.enqueue(MockResponse().setResponseCode(code))
                assertThrows(HttpException::class.java) {
                    runBlocking { repository.api.getAdminSummary() }
                }
                runCurrent()
                val request = server.takeRequest(2, TimeUnit.SECONDS)!!
                assertEquals("Bearer ${fixture.accessToken}", request.getHeader("Authorization"))
                if (code == 403) assertNotNull(repository.state.value.session)
            }
            assertNull(repository.state.value.session)
            assertNull(store.token)
        } finally { server.shutdown() }
    }

    @Test fun loginResponseArrivingAfterLogoutCannotReopenSession() = runTest {
        val server = MockWebServer()
        server.start()
        var pending: CompletableFuture<Unit>? = null
        try {
            server.enqueue(MockResponse().setBody(ApiJson.create().toJson(fixture))
                .setBodyDelay(1, TimeUnit.SECONDS))
            val store = MemoryStore()
            val repository = ApiSessionRepository(store, backgroundScope,
                baseUrl = server.url("/").toString(), clockSeconds = { LoginTestFixtures.NOW })
            runCurrent()
            pending = CompletableFuture.supplyAsync {
                runBlocking { repository.login("demo@energy.test", "Demo1234") }
            }
            assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
            repository.logout()
            val error = assertThrows(ExecutionException::class.java) {
                pending.get(5, TimeUnit.SECONDS)
            }
            assertTrue(error.cause is IllegalStateException)
            assertNull(repository.state.value.session)
            assertNull(store.token)
        } finally {
            pending?.cancel(true)
            server.shutdown()
        }
    }

    @Test fun oldUnauthorizedResponseCannotCloseNewSession() = runTest {
        val server = MockWebServer()
        server.start()
        var pending: CompletableFuture<Unit>? = null
        try {
            server.enqueue(MockResponse().setResponseCode(401)
                .setHeadersDelay(1, TimeUnit.SECONDS))
            val store = MemoryStore(fixture.accessToken)
            val repository = ApiSessionRepository(store, backgroundScope,
                baseUrl = server.url("/").toString(), clockSeconds = { LoginTestFixtures.NOW })
            runCurrent()
            pending = CompletableFuture.supplyAsync {
                assertThrows(HttpException::class.java) {
                    runBlocking { repository.api.getAdminSummary() }
                }
                Unit
            }
            assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
            repository.logout()
            val newLogin = LoginTestFixtures.response(expiresAt = LoginTestFixtures.EXPIRES_AT + 10)
            server.enqueue(MockResponse().setBody(ApiJson.create().toJson(newLogin)))
            runBlocking { repository.login("demo@energy.test", "Demo1234") }
            pending.get(5, TimeUnit.SECONDS)
            runCurrent()
            assertEquals(newLogin.accessToken, repository.state.value.session!!.accessToken)
            assertEquals(newLogin.accessToken, store.token)
        } finally {
            pending?.cancel(true)
            server.shutdown()
        }
    }
}
