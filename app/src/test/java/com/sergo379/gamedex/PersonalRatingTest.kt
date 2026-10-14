package com.sergo379.gamedex

import com.sergo379.gamedex.domain.PersonalRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalRatingTest {
    @Test fun acceptsEveryRequiredHalfStarValue() {
        listOf(0.5 to 1, 1.0 to 2, 1.5 to 3, 2.0 to 4, 2.5 to 5, 3.0 to 6, 3.5 to 7, 4.0 to 8, 4.5 to 9, 5.0 to 10)
            .forEach { (stars, halfStars) -> assertEquals(halfStars, PersonalRating.fromStars(stars)) }
    }

    @Test fun convertsStoredValuesWithoutFloatingPointLoss() {
        assertEquals(0.5, PersonalRating.toStars(1), 0.0)
        assertEquals(2.5, PersonalRating.toStars(5), 0.0)
        assertEquals(4.5, PersonalRating.toStars(9), 0.0)
        assertEquals(5.0, PersonalRating.toStars(10), 0.0)
    }

    @Test fun nullMeansNoRating() {
        PersonalRating.requireValid(null)
        val value: Int? = null
        assertNull(value)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsTooLowStoredValue() = PersonalRating.requireValid(0)
    @Test(expected = IllegalArgumentException::class) fun rejectsTooHighStoredValue() = PersonalRating.requireValid(11)
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedIntermediateValue() { PersonalRating.fromStars(3.7) }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonFiniteValue() { PersonalRating.fromStars(Double.NaN) }
}
