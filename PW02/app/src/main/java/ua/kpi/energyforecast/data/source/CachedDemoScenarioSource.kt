package ua.kpi.energyforecast.data.source

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Зберігає останній успішно прочитаний набір у пам’яті.
 * Один екземпляр використовується протягом роботи застосунку.
 */
class CachedDemoScenarioSource(
    private val source: DemoScenarioSource
) : DemoScenarioSource {

    private val mutex = Mutex()
    private var cached: List<DemoScenario>? = null

    override suspend fun load(): List<DemoScenario> =
        mutex.withLock {
            cached ?: source.load().also { loaded ->
                cached = loaded
            }
        }

    /**
     * Повторно читає джерело.
     * Якщо читання завершується помилкою, попередній кеш зберігається,
     * а помилка передається виклику, що запросив оновлення.
     */
    suspend fun refresh(): List<DemoScenario> =
        mutex.withLock {
            source.load().also { loaded ->
                cached = loaded
            }
        }
}