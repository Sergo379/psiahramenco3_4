package com.sergo379.gamedex.data.local

import com.sergo379.gamedex.data.GameCache
import com.sergo379.gamedex.domain.Game

class RoomGameCache(private val dao: GameDao) : GameCache {
    override suspend fun save(games: List<Game>) {
        dao.upsert(games.map { game -> game.toEntity(dao.get(game.id)) })
    }
    override suspend fun find(query: String, limit: Int): List<Game> = dao.search(query, limit).map { it.toDomain() }
    override suspend fun get(id: Int): Game? = dao.get(id)?.toDomain()
}

