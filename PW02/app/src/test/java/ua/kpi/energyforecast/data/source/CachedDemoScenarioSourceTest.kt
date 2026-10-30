package ua.kpi.energyforecast.data.source

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.model.EnergyDevice

class CachedDemoScenarioSourceTest {

    @Test
    fun repeatedLoadReadsSourceOnce() = runTest {
        var reads = 0
        val expected = scenarios()

        val cache = CachedDemoScenarioSource(
            DemoScenarioSource {
                reads += 1
                expected
            }
        )

        assertEquals(expected, cache.load())
        assertEquals(expected, cache.load())
        assertEquals(1, reads)
    }

    @Test
    fun concurrentLoadsShareOneRead() = runTest {
        var reads = 0
        val expected = scenarios()

        val cache = CachedDemoScenarioSource(
            DemoScenarioSource {
                reads += 1
                delay(100)
                expected
            }
        )

        val results = List(5) {
            async { cache.load() }
        }.awaitAll()

        assertTrue(results.all { it == expected })
        assertEquals(1, reads)
    }

    @Test
    fun failedInitialLoadCanBeRetried() = runTest {
        var reads = 0
        val expected = scenarios()

        val cache = CachedDemoScenarioSource(
            DemoScenarioSource {
                reads += 1
                check(reads > 1) { "Test read failure" }
                expected
            }
        )

        val failure = runCatching { cache.load() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(expected, cache.load())
        assertEquals(2, reads)
    }

    @Test
    fun refreshReplacesCachedData() = runTest {
        var reads = 0
        var current = scenarios("Перша версія")

        val cache = CachedDemoScenarioSource(
            DemoScenarioSource {
                reads += 1
                current
            }
        )

        cache.load()
        current = scenarios("Оновлена версія")

        // Звичайне читання повертає попередній кеш.
        assertEquals("Перша версія", cache.load().single().name)

        assertEquals("Оновлена версія", cache.refresh().single().name)
        assertEquals("Оновлена версія", cache.load().single().name)
        assertEquals(2, reads)
    }

    @Test
    fun failedRefreshPreservesPreviousCache() = runTest {
        var reads = 0
        var failReads = false
        val expected = scenarios()

        val cache = CachedDemoScenarioSource(
            DemoScenarioSource {
                reads += 1
                check(!failReads) { "Test read failure" }
                expected
            }
        )

        cache.load()
        failReads = true

        val failure = runCatching { cache.refresh() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(expected, cache.load())
        assertEquals(2, reads)
    }

    private fun scenarios(
        name: String = "Тестовий приклад"
    ): List<DemoScenario> = listOf(
        DemoScenario(
            id = "test",
            name = name,
            forecastDays = 7,
            devices = listOf(
                EnergyDevice(
                    id = "device",
                    name = "Прилад",
                    powerWatts = 1000.0,
                    hoursPerDay = 2.0,
                    deviceCount = 1
                )
            )
        )
    )
}