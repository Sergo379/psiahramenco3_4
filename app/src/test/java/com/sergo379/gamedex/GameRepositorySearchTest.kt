package com.sergo379.gamedex

import com.sergo379.gamedex.data.GameCache
import com.sergo379.gamedex.data.GameRepository
import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.data.remote.dto.GameDto
import com.sergo379.gamedex.data.remote.dto.GamePageDto
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositorySearchTest {
    @Test fun sendsPrecisePartialSearchWithoutRatingOrdering() = runTest {
        val api = RecordingApi(results = listOf(GameDto(1, "Risk of Rain"), GameDto(9, "The Witcher 3")))
        val repository = GameRepository(api, { "key" }, RecordingCache())
        val result = repository.search("risk of rain", GameFilters(), 1)
        assertEquals(true, api.lastSearchPrecise)
        assertEquals(false, api.lastSearchExact)
        assertNull(api.lastOrdering)
        assertEquals(listOf("Risk of Rain"), result.games.map(Game::name))
    }

    @Test fun forwardsCombinedFilters() = runTest {
        val api = RecordingApi(results = listOf(GameDto(1, "Risk of Rain 2")))
        val filters = GameFilters(genre = "action", platform = 4, year = 2020)
        GameRepository(api, { "key" }, RecordingCache()).search("risk of rain", filters, 1)
        assertEquals("action", api.lastGenres)
        assertEquals(4, api.lastPlatforms)
        assertEquals("2020-01-01,2020-12-31", api.lastDates)
    }

    @Test fun networkErrorReturnsOnlyRelevantCachedGames() = runTest {
        val cache = RecordingCache(mutableListOf(Game(1, "The Witcher 3"), Game(2, "Risk of Rain 2")))
        val result = GameRepository(RecordingApi(error = IOException("offline")), { "key" }, cache)
            .search("risk of rain", GameFilters(), 1)
        assertTrue(result.fromCache)
        assertEquals(listOf("Risk of Rain 2"), result.games.map(Game::name))
    }

    @Test fun networkErrorDoesNotReturnUnrelatedCache() = runTest {
        val cache = RecordingCache(mutableListOf(Game(1, "The Witcher 3")))
        val result = runCatching {
            GameRepository(RecordingApi(error = IOException("offline")), { "key" }, cache)
                .search("risk of rain", GameFilters(), 1)
        }
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test fun cancellationIsRethrownWithoutCacheFallback() = runTest {
        val cache = RecordingCache(mutableListOf(Game(2, "Risk of Rain")))
        val result = runCatching {
            GameRepository(RecordingApi(error = CancellationException("cancelled")), { "key" }, cache)
                .search("risk of rain", GameFilters(), 1)
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(0, cache.findCalls)
    }

    @Test fun loadsRealSeriesAndAdditionsWithoutInventedEntries() = runTest {
        val api = RecordingApi(
            series = listOf(GameDto(2, "Risk of Rain 2")),
            additions = listOf(GameDto(3, "Risk of Rain 2: Survivors of the Void")),
        )
        val result = GameRepository(api, { "key" }, RecordingCache()).related(1)
        assertEquals(listOf("Risk of Rain 2"), result.series.map(Game::name))
        assertEquals(listOf("Risk of Rain 2: Survivors of the Void"), result.additions.map(Game::name))
        assertFalse(result.series.any { it.id == 1 })
    }

    private class RecordingCache(val items: MutableList<Game> = mutableListOf()) : GameCache {
        var findCalls = 0
        override suspend fun save(games: List<Game>) { games.forEach { game -> items.removeAll { it.id == game.id }; items += game } }
        override suspend fun find(query: String, limit: Int): List<Game> { findCalls++; return items.filter { it.name.contains(query, true) }.take(limit) }
        override suspend fun get(id: Int): Game? = items.firstOrNull { it.id == id }
    }

    private class RecordingApi(
        private val results: List<GameDto> = emptyList(),
        private val error: Throwable? = null,
        private val series: List<GameDto> = emptyList(),
        private val additions: List<GameDto> = emptyList(),
    ) : RawgApi {
        var lastSearchPrecise: Boolean? = null
        var lastSearchExact: Boolean? = null
        var lastOrdering: String? = null
        var lastGenres: String? = null
        var lastPlatforms: Int? = null
        var lastDates: String? = null

        override suspend fun games(
            key: String,
            search: String?,
            searchPrecise: Boolean?,
            searchExact: Boolean?,
            genres: String?,
            platforms: Int?,
            dates: String?,
            ordering: String?,
            page: Int,
            pageSize: Int,
        ): GamePageDto {
            error?.let { throw it }
            lastSearchPrecise = searchPrecise
            lastSearchExact = searchExact
            lastOrdering = ordering
            lastGenres = genres
            lastPlatforms = platforms
            lastDates = dates
            return GamePageDto(results.size, null, results)
        }

        override suspend fun game(id: Int, key: String) = GameDto(id, "Game")
        override suspend fun additions(gameId: Int, key: String, pageSize: Int) = GamePageDto(additions.size, null, additions)
        override suspend fun gameSeries(gameId: Int, key: String, pageSize: Int) = GamePageDto(series.size, null, series)
    }
}
