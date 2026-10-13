package com.sergo379.gamedex.domain

object InputValidator {
    const val MAX_QUERY_LENGTH = 80

    fun validateQuery(value: String): String? = when {
        value.isBlank() -> "Введите название игры"
        value.trim().length > MAX_QUERY_LENGTH -> "Запрос слишком длинный"
        else -> null
    }

    fun validateYear(value: String): String? {
        if (value.isBlank()) return null
        val year = value.toIntOrNull() ?: return "Год должен быть числом"
        return if (year !in 1950..2100) "Укажите год от 1950 до 2100" else null
    }

    fun filtersAreValid(filters: GameFilters): Boolean = filters.year == null || filters.year in 1950..2100
}

