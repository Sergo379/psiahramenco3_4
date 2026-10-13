package com.sergo379.gamedex.domain

import java.time.Clock
import java.time.LocalDate

data class AnalyticsPoint(val date: LocalDate, val count: Int?, val fetchedAt: Long? = null)
enum class Trend { UP, DOWN, FLAT, UNKNOWN }

data class AnalyticsStats(
    val minimum: Int?,
    val maximum: Int?,
    val average: Double?,
    val delta: Int?,
    val percentage: Double?,
    val trend: Trend,
    val complete: Boolean,
    val knownValues: Int,
    val totalValues: Int,
)

object StatisticsCalculator {
    fun calculate(points: List<AnalyticsPoint>): AnalyticsStats {
        val sorted = points.sortedBy { it.date }
        val known = sorted.mapNotNull { it.count }
        val first = sorted.firstOrNull()?.count
        val last = sorted.lastOrNull()?.count
        val delta = if (first != null && last != null) last - first else null
        val percentage = when {
            first == null || last == null -> null
            first == 0 && last == 0 -> 0.0
            first == 0 -> null
            else -> delta!! * 100.0 / first
        }
        val trend = when {
            delta == null -> Trend.UNKNOWN
            delta > 0 -> Trend.UP
            delta < 0 -> Trend.DOWN
            else -> Trend.FLAT
        }
        return AnalyticsStats(
            minimum = known.minOrNull(),
            maximum = known.maxOrNull(),
            average = known.takeIf { it.isNotEmpty() }?.average(),
            delta = delta,
            percentage = percentage,
            trend = trend,
            complete = points.isNotEmpty() && known.size == points.size,
            knownValues = known.size,
            totalValues = points.size,
        )
    }

    fun completedDates(days: Int, clock: Clock = Clock.systemDefaultZone()): List<LocalDate> {
        require(days > 0)
        val yesterday = LocalDate.now(clock).minusDays(1)
        return (days - 1 downTo 0).map { yesterday.minusDays(it.toLong()) }
    }

    fun latest(points: List<AnalyticsPoint>, days: Int): List<AnalyticsPoint> =
        points.sortedBy { it.date }.takeLast(days)
}

