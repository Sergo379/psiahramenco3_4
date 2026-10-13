package com.sergo379.gamedex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.PlayStatus

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val released: String?,
    val backgroundImage: String?,
    val rating: Double,
    val metacritic: Int?,
    val genres: String,
    val platforms: String,
    val developers: String,
    val publishers: String,
    val website: String?,
    val screenshots: String,
    val favorite: Boolean = false,
    val playStatus: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)

private const val SEP = "\u001F"

fun Game.toEntity(previous: GameEntity? = null) = GameEntity(
    id, name, description, released, backgroundImage, rating, metacritic,
    genres.joinToString(SEP), platforms.joinToString(SEP), developers.joinToString(SEP),
    publishers.joinToString(SEP), website, screenshots.joinToString(SEP),
    favorite = previous?.favorite ?: false,
    playStatus = previous?.playStatus,
)

fun GameEntity.toDomain() = Game(
    id, name, description, released, backgroundImage, rating, metacritic,
    genres.split(SEP).filter(String::isNotBlank), platforms.split(SEP).filter(String::isNotBlank),
    developers.split(SEP).filter(String::isNotBlank), publishers.split(SEP).filter(String::isNotBlank),
    website, screenshots.split(SEP).filter(String::isNotBlank),
)

data class LibraryGame(val game: Game, val favorite: Boolean, val status: PlayStatus?)
fun GameEntity.toLibraryGame() = LibraryGame(toDomain(), favorite, playStatus?.let { runCatching { PlayStatus.valueOf(it) }.getOrNull() })

