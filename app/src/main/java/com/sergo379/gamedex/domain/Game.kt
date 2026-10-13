package com.sergo379.gamedex.domain

data class Game(
    val id: Int,
    val name: String,
    val description: String = "",
    val released: String? = null,
    val backgroundImage: String? = null,
    val rating: Double = 0.0,
    val metacritic: Int? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val website: String? = null,
    val screenshots: List<String> = emptyList(),
) {
    val year: String get() = released?.take(4) ?: "—"
}

data class GameFilters(
    val genre: String? = null,
    val platform: Int? = null,
    val year: Int? = null,
    val ordering: String = "-rating",
)

enum class PlayStatus(val label: String) {
    WANT_TO_PLAY("Хочу пройти"),
    PLAYING("Прохожу"),
    COMPLETED("Пройдено"),
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T, val fromCache: Boolean = false) : UiState<T>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String, val canRetry: Boolean = true) : UiState<Nothing>
}

