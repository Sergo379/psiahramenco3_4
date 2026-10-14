package com.sergo379.gamedex.data

import com.sergo379.gamedex.data.local.GameDao
import com.sergo379.gamedex.data.local.LibraryGame
import com.sergo379.gamedex.data.local.toLibraryGame
import com.sergo379.gamedex.domain.PlayStatus
import com.sergo379.gamedex.domain.PersonalRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepository(
    private val dao: GameDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observe(): Flow<List<LibraryGame>> = dao.observeLibrary().map { rows -> rows.map { it.toLibraryGame() } }
    suspend fun item(id: Int): LibraryGame? = dao.get(id)?.toLibraryGame()
    suspend fun toggleFavorite(id: Int): Boolean {
        val row = dao.get(id) ?: return false
        val newValue = !row.favorite
        dao.setFavorite(id, newValue, now())
        return newValue
    }
    suspend fun setStatus(id: Int, status: PlayStatus?) = dao.setStatus(id, status?.name, now())
    suspend fun userRating(id: Int): Int? = dao.get(id)?.userRatingHalfStars
    suspend fun setUserRating(id: Int, ratingHalfStars: Int?) {
        PersonalRating.requireValid(ratingHalfStars)
        dao.setUserRating(id, ratingHalfStars, now())
    }
    suspend fun removeUserRating(id: Int) = setUserRating(id, null)
}

