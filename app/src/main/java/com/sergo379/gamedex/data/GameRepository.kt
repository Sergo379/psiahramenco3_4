package com.sergo379.gamedex.data

import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.data.remote.dto.toDomain
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.RelatedGames
import com.sergo379.gamedex.domain.SearchRelevance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface GameCache {
    suspend fun save(games: List<Game>)
    suspend fun find(query: String, limit: Int = 40): List<Game>
    suspend fun get(id: Int): Game?
}

interface GameSearchDataSource {
    suspend fun search(query: String, filters: GameFilters, page: Int): PageResult
}

class MemoryGameCache : GameCache {
    private val mutex = Mutex()
    private val items = linkedMapOf<Int, Game>()
    override suspend fun save(games: List<Game>) = mutex.withLock { games.forEach { items[it.id] = it } }
    override suspend fun find(query: String, limit: Int) = mutex.withLock {
        items.values.filter { it.name.contains(query, ignoreCase = true) }.take(limit)
    }
    override suspend fun get(id: Int) = mutex.withLock { items[id] }
}

data class PageResult(val games: List<Game>, val hasNext: Boolean, val fromCache: Boolean)

class GameRepository(
    private val api: RawgApi,
    private val apiKey: suspend () -> String,
    private val cache: GameCache,
    private val onUpdated: suspend () -> Unit = {},
) : GameSearchDataSource {
    suspend fun popular(): PageResult {
        val key = apiKey().trim()
        if (key.isEmpty()) return cachedPopular()
        return try {
            val response = api.games(key = key, ordering = "-added", page = 1)
            val games = response.results.map { it.toDomain() }
            cache.save(games)
            onUpdated()
            PageResult(games, response.next != null, false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val cached = cache.find("", 40)
            if (cached.isNotEmpty()) PageResult(cached, false, true) else throw error
        }
    }

    override suspend fun search(query: String, filters: GameFilters, page: Int): PageResult {
        val cleanQuery = query.trim()
        require(cleanQuery.isNotEmpty()) { "Search query must not be blank" }
        val key = apiKey().trim()
        if (key.isEmpty()) return cachedSearch(cleanQuery)
        return try {
            val yearRange = filters.year?.let { "$it-01-01,$it-12-31" }
            val response = api.games(
                key = key,
                search = cleanQuery,
                searchPrecise = true,
                searchExact = false,
                genres = filters.genre,
                platforms = filters.platform,
                dates = yearRange,
                ordering = filters.ordering,
                page = page,
            )
            val received = response.results.map { it.toDomain() }
            cache.save(received)
            onUpdated()
            PageResult(SearchRelevance.rank(cleanQuery, received), response.next != null, false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val cached = relevantCachedGames(cleanQuery)
            if (cached.isNotEmpty()) PageResult(cached, false, true) else throw error
        }
    }

    suspend fun details(id: Int): Pair<Game, Boolean> {
        val key = apiKey().trim()
        if (key.isEmpty()) return cache.get(id)?.let { it to true }
            ?: throw IllegalStateException("Добавьте RAWG API-ключ в настройках")
        return try {
            val game = api.game(id, key).toDomain()
            cache.save(listOf(game))
            onUpdated()
            game to false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            cache.get(id)?.let { it to true } ?: throw error
        }
    }

    suspend fun related(id: Int): RelatedGames {
        val key = apiKey().trim()
        if (key.isEmpty()) return RelatedGames()
        return coroutineScope {
            val series = async { relatedPage { api.gameSeries(id, key) } }
            val additions = async { relatedPage { api.additions(id, key) } }
            val result = RelatedGames(
                series = series.await().filterNot { it.id == id },
                additions = additions.await().filterNot { it.id == id },
            )
            cache.save((result.series + result.additions).distinctBy(Game::id))
            result
        }
    }

    private suspend fun relatedPage(request: suspend () -> com.sergo379.gamedex.data.remote.dto.GamePageDto): List<Game> = try {
        request().results.map { it.toDomain() }.distinctBy(Game::id)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        emptyList()
    }

    private suspend fun relevantCachedGames(query: String): List<Game> =
        SearchRelevance.rank(query, cache.find("", 200))

    private suspend fun cachedSearch(query: String): PageResult {
        val cached = relevantCachedGames(query)
        if (cached.isNotEmpty()) return PageResult(cached, false, true)
        throw IllegalStateException("Добавьте RAWG API-ключ в настройках")
    }

    private suspend fun cachedPopular(): PageResult {
        val cached = cache.find("", 40)
        if (cached.isNotEmpty()) return PageResult(cached, false, true)
        throw IllegalStateException("Добавьте RAWG API-ключ в настройках")
    }
}
