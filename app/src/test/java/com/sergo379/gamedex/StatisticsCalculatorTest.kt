package com.sergo379.gamedex

import com.sergo379.gamedex.domain.AnalyticsPoint
import com.sergo379.gamedex.domain.StatisticsCalculator
import com.sergo379.gamedex.domain.Trend
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class StatisticsCalculatorTest {
    private val start = LocalDate.of(2026, 9, 1)
    private fun points(vararg values: Int?) = values.mapIndexed { index, value -> AnalyticsPoint(start.plusDays(index.toLong()), value) }

    @Test fun minimum() = assertEquals(2, StatisticsCalculator.calculate(points(4, 2, 9)).minimum)
    @Test fun maximum() = assertEquals(9, StatisticsCalculator.calculate(points(4, 2, 9)).maximum)
    @Test fun average() = assertEquals(5.0, StatisticsCalculator.calculate(points(4, 2, 9)).average!!, .001)
    @Test fun positiveChange() { val s=StatisticsCalculator.calculate(points(2, 5)); assertEquals(3,s.delta); assertEquals(Trend.UP,s.trend) }
    @Test fun negativeChange() { val s=StatisticsCalculator.calculate(points(8, 3)); assertEquals(-5,s.delta); assertEquals(Trend.DOWN,s.trend) }
    @Test fun noChange() { val s=StatisticsCalculator.calculate(points(4, 4)); assertEquals(0,s.delta); assertEquals(Trend.FLAT,s.trend) }
    @Test fun percentageChange() = assertEquals(50.0, StatisticsCalculator.calculate(points(10, 15)).percentage!!, .001)
    @Test fun divisionByZeroIsUndefined() = assertNull(StatisticsCalculator.calculate(points(0, 5)).percentage)
    @Test fun zeroToZeroIsZeroPercent() = assertEquals(0.0, StatisticsCalculator.calculate(points(0, 0)).percentage!!, .001)
    @Test fun emptyData() { val s=StatisticsCalculator.calculate(emptyList()); assertNull(s.average); assertFalse(s.complete) }
    @Test fun singleValue() { val s=StatisticsCalculator.calculate(points(7)); assertEquals(7,s.minimum); assertEquals(0,s.delta) }
    @Test fun incompleteSeries() { val s=StatisticsCalculator.calculate(points(1, null, 3)); assertFalse(s.complete); assertEquals(2,s.knownValues) }
    @Test fun sortsDatesForDelta() {
        val list=listOf(AnalyticsPoint(start.plusDays(1), 8),AnalyticsPoint(start, 3)); assertEquals(5,StatisticsCalculator.calculate(list).delta)
    }
    @Test fun selectsLastSeven() = assertEquals(7, StatisticsCalculator.latest(points(*Array(30){it}), 7).size)
    @Test fun selectsLastThirty() = assertEquals(30, StatisticsCalculator.latest(points(*Array(35){it}), 30).size)
    @Test fun completedDatesExcludeCurrentDay() {
        val clock=Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)
        val dates=StatisticsCalculator.completedDates(7,clock); assertEquals(LocalDate.of(2026,10,12),dates.last()); assertEquals(7,dates.size)
    }
}

