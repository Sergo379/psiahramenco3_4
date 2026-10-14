package com.sergo379.gamedex

import com.sergo379.gamedex.data.LibraryOrganizer
import com.sergo379.gamedex.data.local.LibraryGame
import com.sergo379.gamedex.domain.Game
import com.sergo379.gamedex.domain.LibrarySortMode
import com.sergo379.gamedex.domain.PlayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryOrganizerTest {
    private fun item(
        id: Int,
        name: String,
        rating: Int? = null,
        addedAt: Long? = null,
        status: PlayStatus? = null,
    ) = LibraryGame(
        game = Game(id, name),
        favorite = true,
        status = status,
        userRatingHalfStars = rating,
        addedAt = addedAt,
    )

    private val games = listOf(
        item(1, "Ярость", 10, 100, PlayStatus.COMPLETED),
        item(2, "Alpha", 6, 300, PlayStatus.PLAYING),
        item(3, "Бастион", 9, 200, PlayStatus.COMPLETED),
        item(4, "beta", null, null, PlayStatus.COMPLETED),
        item(5, "hollow knight", 10, 400, PlayStatus.WANT_TO_PLAY),
    )

    @Test fun ratingDescendingKeepsUnratedLastAndUsesTitleAsTieBreaker() {
        assertEquals(listOf(5, 1, 3, 2, 4), sorted(LibrarySortMode.RATING_DESC))
    }

    @Test fun ratingAscendingKeepsUnratedLast() {
        assertEquals(listOf(2, 3, 5, 1, 4), sorted(LibrarySortMode.RATING_ASC))
    }

    @Test fun newestAddedComesFirstAndUnknownDateLast() {
        assertEquals(listOf(5, 2, 3, 1, 4), sorted(LibrarySortMode.ADDED_NEWEST))
    }

    @Test fun alphabeticSortingIsCaseInsensitiveAndDeterministic() {
        assertEquals(listOf(2, 4, 5, 3, 1), sorted(LibrarySortMode.ALPHABETICAL))
    }

    @Test fun searchAndStatusFilterAreAppliedTogetherWithSorting() {
        val result = LibraryOrganizer.apply(games, "с", PlayStatus.COMPLETED, LibrarySortMode.RATING_DESC)
        assertEquals(listOf(1, 3), result.map { it.game.id })
    }

    @Test fun emptyLibraryStaysEmpty() {
        assertTrue(LibraryOrganizer.apply(emptyList(), "", null, LibrarySortMode.RATING_DESC).isEmpty())
    }

    @Test fun oneGameLibraryStaysStable() {
        val one = listOf(item(8, "Only", 7, 1))
        assertEquals(one, LibraryOrganizer.apply(one, "", null, LibrarySortMode.ALPHABETICAL))
    }

    @Test fun sameRatingAndSameTitleUseIdAsStableFinalKey() {
        val ties = listOf(item(9, "Same", 8), item(7, "same", 8))
        val result = LibraryOrganizer.apply(ties, "", null, LibrarySortMode.RATING_DESC)
        assertEquals(listOf(7, 9), result.map { it.game.id })
    }

    private fun sorted(mode: LibrarySortMode) =
        LibraryOrganizer.apply(games, "", null, mode).map { it.game.id }
}
