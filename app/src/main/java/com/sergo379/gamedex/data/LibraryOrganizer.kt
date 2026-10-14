package com.sergo379.gamedex.data

import com.sergo379.gamedex.data.local.LibraryGame
import com.sergo379.gamedex.domain.LibrarySortMode
import com.sergo379.gamedex.domain.PlayStatus
import java.text.Collator
import java.util.Locale

object LibraryOrganizer {
    fun apply(
        games: List<LibraryGame>,
        query: String,
        status: PlayStatus?,
        sort: LibrarySortMode,
    ): List<LibraryGame> {
        val filtered = games.filter {
            it.game.name.contains(query.trim(), ignoreCase = true) &&
                (status == null || it.status == status)
        }
        val titleCollator = Collator.getInstance(Locale("ru", "RU")).apply {
            strength = Collator.PRIMARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }
        val titleThenId = Comparator<LibraryGame> { left, right ->
            val scriptOrder = scriptBucket(left.game.name).compareTo(scriptBucket(right.game.name))
            if (scriptOrder != 0) scriptOrder
            else titleCollator.compare(left.game.name, right.game.name)
                .takeIf { it != 0 } ?: left.game.id.compareTo(right.game.id)
        }
        val comparator = when (sort) {
            LibrarySortMode.ALPHABETICAL -> titleThenId
            LibrarySortMode.ADDED_NEWEST -> Comparator { left, right ->
                when {
                    left.addedAt == null && right.addedAt != null -> 1
                    left.addedAt != null && right.addedAt == null -> -1
                    left.addedAt != right.addedAt -> compareValues(right.addedAt, left.addedAt)
                    else -> titleThenId.compare(left, right)
                }
            }
            LibrarySortMode.RATING_DESC, LibrarySortMode.RATING_ASC -> Comparator { left, right ->
                val leftRating = left.userRatingHalfStars
                val rightRating = right.userRatingHalfStars
                when {
                    leftRating == null && rightRating != null -> 1
                    leftRating != null && rightRating == null -> -1
                    leftRating != rightRating && sort == LibrarySortMode.RATING_DESC ->
                        compareValues(rightRating, leftRating)
                    leftRating != rightRating -> compareValues(leftRating, rightRating)
                    else -> titleThenId.compare(left, right)
                }
            }
        }
        return filtered.sortedWith(comparator)
    }

    private fun scriptBucket(value: String): Int {
        val first = value.trim().firstOrNull()?.lowercaseChar() ?: return 2
        return when (first) {
            in 'a'..'z' -> 0
            in 'а'..'я', 'ё' -> 1
            else -> 2
        }
    }
}
