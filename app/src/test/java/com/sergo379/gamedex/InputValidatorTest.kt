package com.sergo379.gamedex

import com.sergo379.gamedex.domain.GameFilters
import com.sergo379.gamedex.domain.InputValidator
import org.junit.Assert.*
import org.junit.Test

class InputValidatorTest {
    @Test fun emptyQueryRejected() = assertNotNull(InputValidator.validateQuery(""))
    @Test fun whitespaceQueryRejected() = assertNotNull(InputValidator.validateQuery("   "))
    @Test fun validGameNameAccepted() = assertNull(InputValidator.validateQuery("Hollow Knight"))
    @Test fun longQueryRejected() = assertNotNull(InputValidator.validateQuery("x".repeat(81)))
    @Test fun invalidYearRejected() { assertNotNull(InputValidator.validateYear("abc")); assertNotNull(InputValidator.validateYear("1800")) }
    @Test fun validFiltersAccepted() = assertTrue(InputValidator.filtersAreValid(GameFilters("action", 4, 2024)))
}

