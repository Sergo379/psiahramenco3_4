package com.sergo379.gamedex

import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.SearchRelevance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRelevanceTest {
    private val catalog = listOf(
        Game(1, "The Witcher 3: Wild Hunt", rating = 4.7),
        Game(2, "Risk of Rain", rating = 4.2),
        Game(3, "Risk of Rain 2", rating = 4.4),
        Game(4, "Risk of Rain Returns", rating = 4.1),
        Game(5, "Hollow Knight", rating = 4.6),
        Game(6, "Half-Life 2", rating = 4.5),
        Game(7, "BioShock Infinite", rating = 4.4),
        Game(8, "Elden Ring", rating = 4.8),
    )

    @Test fun riskOfRainExcludesUnrelatedPopularGames() {
        val results = SearchRelevance.rank("risk of rain", catalog)
        assertEquals(listOf(2, 3, 4), results.map(Game::id))
        assertFalse(results.any { it.name == "The Witcher 3: Wild Hunt" })
    }

    @Test fun exactTitleRanksBeforeSeriesEntries() {
        assertEquals("Risk of Rain 2", SearchRelevance.rank("Risk of Rain 2", catalog).first().name)
    }

    @Test fun punctuationAndCaseDoNotBreakMatching() {
        assertTrue(SearchRelevance.isRelevant("half life 2", "Half-Life 2"))
        assertTrue(SearchRelevance.isRelevant("bioshock infinite", "BioShock Infinite"))
    }

    @Test fun requiredRegressionQueriesFindExpectedTitles() {
        val queries = listOf(
            "risk of rain" to "Risk of Rain",
            "Risk of Rain 2" to "Risk of Rain 2",
            "Risk of Rain Returns" to "Risk of Rain Returns",
            "Hollow Knight" to "Hollow Knight",
            "The Witcher 3" to "The Witcher 3: Wild Hunt",
            "Half-Life 2" to "Half-Life 2",
            "BioShock Infinite" to "BioShock Infinite",
            "elden ring" to "Elden Ring",
        )
        queries.forEach { (query, expected) -> assertTrue("$query should find $expected", SearchRelevance.rank(query, catalog).any { it.name == expected }) }
    }

    @Test fun duplicateIdsAreRemoved() {
        val results = SearchRelevance.rank("risk of rain", catalog + catalog.first { it.id == 2 })
        assertEquals(results.map(Game::id).distinct(), results.map(Game::id))
    }
}

