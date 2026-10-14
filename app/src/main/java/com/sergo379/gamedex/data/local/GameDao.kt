package com.sergo379.gamedex.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Upsert suspend fun upsert(games: List<GameEntity>)
    @Query("SELECT * FROM games WHERE id = :id") suspend fun get(id: Int): GameEntity?

    @Transaction
    suspend fun upsertPreservingLocal(game: GameEntity) {
        val previous = get(game.id)
        upsert(
            listOf(
                game.copy(
                    favorite = previous?.favorite ?: false,
                    playStatus = previous?.playStatus,
                    userRatingHalfStars = previous?.userRatingHalfStars,
                    addedAt = previous?.addedAt,
                ),
            ),
        )
    }

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT :limit") suspend fun search(query: String, limit: Int): List<GameEntity>
    @Query("SELECT * FROM games WHERE favorite = 1 OR playStatus IS NOT NULL OR userRatingHalfStars IS NOT NULL ORDER BY COALESCE(addedAt, 0) DESC")
    fun observeLibrary(): Flow<List<GameEntity>>

    @Query("UPDATE games SET favorite = :favorite, addedAt = CASE WHEN :favorite = 1 AND addedAt IS NULL THEN :now ELSE addedAt END WHERE id = :id")
    suspend fun updateFavoriteAndAddedAt(id: Int, favorite: Boolean, now: Long)

    @Query("UPDATE games SET playStatus = :status, addedAt = CASE WHEN :status IS NOT NULL AND addedAt IS NULL THEN :now ELSE addedAt END WHERE id = :id")
    suspend fun updateStatusAndAddedAt(id: Int, status: String?, now: Long)

    @Query("UPDATE games SET userRatingHalfStars = :rating, addedAt = CASE WHEN :rating IS NOT NULL AND addedAt IS NULL THEN :now ELSE addedAt END WHERE id = :id")
    suspend fun updateRatingAndAddedAt(id: Int, rating: Int?, now: Long)

    @Transaction
    suspend fun setFavorite(id: Int, favorite: Boolean, now: Long) = updateFavoriteAndAddedAt(id, favorite, now)

    @Transaction
    suspend fun setStatus(id: Int, status: String?, now: Long) = updateStatusAndAddedAt(id, status, now)

    @Transaction
    suspend fun setUserRating(id: Int, rating: Int?, now: Long) = updateRatingAndAddedAt(id, rating, now)

    @Query("SELECT MAX(cachedAt) FROM games") suspend fun lastUpdated(): Long?
}

