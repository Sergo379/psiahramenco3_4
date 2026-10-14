package com.sergo379.gamedex

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.sergo379.gamedex.ui.components.PersonalRatingStars
import com.sergo379.gamedex.ui.theme.GameDexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PersonalRatingStarsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tappingLeftHalfOfFourthStarSelectsThreeAndAHalf() {
        var selected by mutableStateOf<Int?>(null)
        compose.setContent {
            GameDexTheme { PersonalRatingStars(selected, onValueChange = { selected = it }) }
        }

        compose.onNodeWithTag("rating-7").performClick()
        compose.runOnIdle { assertEquals(7, selected) }
        compose.onNodeWithTag("personal-rating-stars").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Моя оценка 3,5 из 5"),
        )
    }

    @Test fun screenReaderCanSelectFiveStars() {
        var selected by mutableStateOf<Int?>(1)
        compose.setContent {
            GameDexTheme { PersonalRatingStars(selected, onValueChange = { selected = it }) }
        }

        compose.onNodeWithTag("rating-10").performClick()
        compose.runOnIdle { assertEquals(10, selected) }
    }
}
