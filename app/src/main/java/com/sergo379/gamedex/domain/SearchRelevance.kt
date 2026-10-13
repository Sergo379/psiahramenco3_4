package com.sergo379.gamedex.domain

import java.util.Locale

object SearchRelevance {
    private val ignoredWords = setOf("a", "an", "and", "of", "the")

    fun rank(query: String, games: List<Game>): List<Game> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return games.distinctBy(Game::id)
        return games
            .distinctBy(Game::id)
            .mapNotNull { game -> relevanceGroup(normalizedQuery, game.name)?.let { group -> Ranked(game, group) } }
            .sortedWith(
                compareBy<Ranked> { it.group }
                    .thenByDescending { it.game.rating }
                    .thenByDescending { it.game.metacritic ?: -1 }
                    .thenBy { normalize(it.game.name).length }
                    .thenBy { it.game.name },
            )
            .map(Ranked::game)
    }

    fun isRelevant(query: String, name: String): Boolean = relevanceGroup(normalize(query), name) != null

    private fun relevanceGroup(normalizedQuery: String, name: String): Int? {
        if (normalizedQuery.isBlank()) return 0
        val normalizedName = normalize(name)
        return when {
            normalizedName == normalizedQuery -> 0
            normalizedName.startsWith(normalizedQuery) -> 1
            normalizedName.contains(normalizedQuery) -> 2
            meaningfulTokens(normalizedQuery).all { token -> token in normalizedName.split(' ') } -> 3
            else -> null
        }
    }

    private fun meaningfulTokens(value: String): List<String> {
        val tokens = value.split(' ').filter(String::isNotBlank)
        return tokens.filterNot { it in ignoredWords }.ifEmpty { tokens }
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private data class Ranked(val game: Game, val group: Int)
}
