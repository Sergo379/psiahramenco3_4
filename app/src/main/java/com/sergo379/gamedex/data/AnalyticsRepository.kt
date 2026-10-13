package com.sergo379.gamedex.data

import com.sergo379.gamedex.data.local.AnalyticsDao
import com.sergo379.gamedex.data.local.AnalyticsEntity
import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.domain.AnalyticsPoint
import com.sergo379.gamedex.domain.StatisticsCalculator
import java.time.Clock
import java.time.LocalDate
import java.util.Collections
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException

data class AnalyticsResult(
    val points: List<AnalyticsPoint>,
    val fromCache: Boolean,
    val failedDates: List<LocalDate>,
    val updatedAt: Long?,
)

class AnalyticsRepository(
    private val api: RawgApi,
    private val dao: AnalyticsDao,
    private val apiKey: suspend () -> String,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val freshnessMs = 6 * 60 * 60 * 1000L

    suspend fun load(days: Int = 30, force: Boolean = false): AnalyticsResult = coroutineScope {
        val dates = StatisticsCalculator.completedDates(days, clock)
        val cached = dao.between(dates.first().toString(), dates.last().toString()).associateBy { it.date }
        val staleDates = dates.filter { date ->
            val row = cached[date.toString()]
            force || row == null || now() - row.fetchedAt > freshnessMs
        }
        val key = apiKey().trim()
        val failures = Collections.synchronizedList(mutableListOf<LocalDate>())
        val fresh = if (key.isBlank()) {
            failures += staleDates
            emptyList()
        } else {
            val semaphore = Semaphore(4)
            staleDates.map { date -> async {
                semaphore.withPermit {
                    runCatching { fetchDate(date, key) }.getOrElse { failures += date; null }
                }
            } }.awaitAll().filterNotNull()
        }
        if (fresh.isNotEmpty()) dao.upsert(fresh)
        val saved = dao.between(dates.first().toString(), dates.last().toString()).associateBy { it.date }
        val points = dates.map { date -> saved[date.toString()]?.let { AnalyticsPoint(date, it.count, it.fetchedAt) } ?: AnalyticsPoint(date, null) }
        AnalyticsResult(points, fresh.isEmpty() && saved.isNotEmpty(), failures.toList(), saved.values.maxOfOrNull { it.fetchedAt })
    }

    private suspend fun fetchDate(date: LocalDate, key: String): AnalyticsEntity {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                val value = api.games(key = key, dates = "$date,$date", pageSize = 1).count
                    ?: error("RAWG response does not contain count")
                require(value >= 0) { "RAWG count must be non-negative" }
                return AnalyticsEntity(date.toString(), value, now())
            } catch (error: Throwable) {
                lastError = error
                if (error is HttpException && error.code() == 429 && attempt < 2) delay(1_000L * (attempt + 1))
                else throw error
            }
        }
        throw requireNotNull(lastError)
    }
}
