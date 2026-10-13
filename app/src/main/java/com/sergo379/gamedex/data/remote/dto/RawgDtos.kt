package com.sergo379.gamedex.data.remote.dto

import com.sergo379.gamedex.domain.Game
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamePageDto(
    val count: Int? = null,
    val next: String? = null,
    val results: List<GameDto> = emptyList(),
)

@Serializable
data class GameDto(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("description_raw") val descriptionRaw: String = "",
    val released: String? = null,
    @SerialName("background_image") val backgroundImage: String? = null,
    val rating: Double = 0.0,
    val metacritic: Int? = null,
    val genres: List<NamedDto> = emptyList(),
    val platforms: List<PlatformWrapperDto> = emptyList(),
    val developers: List<NamedDto> = emptyList(),
    val publishers: List<NamedDto> = emptyList(),
    val website: String? = null,
    @SerialName("short_screenshots") val shortScreenshots: List<ScreenshotDto> = emptyList(),
)

@Serializable data class NamedDto(val id: Int = 0, val name: String = "")
@Serializable data class PlatformWrapperDto(val platform: NamedDto)
@Serializable data class ScreenshotDto(val id: Int = 0, val image: String = "")

fun GameDto.toDomain() = Game(
    id = id,
    name = name,
    description = descriptionRaw.ifBlank { description.replace(Regex("<[^>]*>"), "") },
    released = released,
    backgroundImage = backgroundImage,
    rating = rating.coerceIn(0.0, 5.0),
    metacritic = metacritic?.takeIf { it in 0..100 },
    genres = genres.mapNotNull { it.name.takeIf(String::isNotBlank) },
    platforms = platforms.mapNotNull { it.platform.name.takeIf(String::isNotBlank) },
    developers = developers.mapNotNull { it.name.takeIf(String::isNotBlank) },
    publishers = publishers.mapNotNull { it.name.takeIf(String::isNotBlank) },
    website = website?.takeIf { it.startsWith("https://") },
    screenshots = shortScreenshots.mapNotNull { it.image.takeIf(String::isNotBlank) },
)

