package com.sergo379.gamedex.data

import com.sergo379.gamedex.data.remote.RawgApi
import com.sergo379.gamedex.data.remote.dto.toDomain
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.GameFilters
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface GameCache {
    suspend fun save(games: List<Game>)
    suspend fun find(query: String, limit: Int = 40): List<Game>
    suspend fun get(id: Int): Game?
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
) {
    suspend fun popular(): PageResult = loadPage(search = null, filters = GameFilters(ordering = "-added"), page = 1)

    suspend fun search(query: String, filters: GameFilters, page: Int): PageResult =
        loadPage(query.trim(), filters, page)

    private suspend fun loadPage(search: String?, filters: GameFilters, page: Int): PageResult {
        val key = apiKey().trim()
        if (key.isEmpty()) {
            val cached = cache.find(search.orEmpty())
            if (cached.isNotEmpty()) return PageResult(cached, false, true)
            throw IllegalStateException("Добавьте RAWG API-ключ в настройках")
        }
        return try {
            val yearRange = filters.year?.let { "$it-01-01,$it-12-31" }
            val response = api.games(
                key = key,
                search = search?.takeIf(String::isNotBlank),
                genres = filters.genre,
                platforms = filters.platform,
                dates = yearRange,
                ordering = filters.ordering,
                page = page,
            )
            val games = response.results.map { it.toDomain() }
            cache.save(games)
            PageResult(games, response.next != null, false)
        } catch (error: Throwable) {
            val cached = cache.find(search.orEmpty())
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
            game to false
        } catch (error: Throwable) {
            cache.get(id)?.let { it to true } ?: throw error
        }
    }
}
