package com.sergo379.gamedex.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Upsert suspend fun upsert(games: List<GameEntity>)
    @Query("SELECT * FROM games WHERE id = :id") suspend fun get(id: Int): GameEntity?
    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT :limit") suspend fun search(query: String, limit: Int): List<GameEntity>
    @Query("SELECT * FROM games WHERE favorite = 1 OR playStatus IS NOT NULL ORDER BY cachedAt DESC") fun observeLibrary(): Flow<List<GameEntity>>
    @Query("UPDATE games SET favorite = :favorite WHERE id = :id") suspend fun setFavorite(id: Int, favorite: Boolean)
    @Query("UPDATE games SET playStatus = :status WHERE id = :id") suspend fun setStatus(id: Int, status: String?)
    @Query("SELECT MAX(cachedAt) FROM games") suspend fun lastUpdated(): Long?
}

