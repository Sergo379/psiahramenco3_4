package com.sergo379.gamedex.domain

import kotlin.math.roundToInt

object PersonalRating {
    const val MIN_HALF_STARS = 1
    const val MAX_HALF_STARS = 10

    fun requireValid(value: Int?) {
        require(value == null || value in MIN_HALF_STARS..MAX_HALF_STARS) {
            "Personal rating must be null or between 1 and 10 half-stars"
        }
    }

    fun fromStars(stars: Double): Int {
        require(stars.isFinite()) { "Personal rating must be finite" }
        val halfStars = (stars * 2).roundToInt()
        require(stars in 0.5..5.0 && kotlin.math.abs(stars * 2 - halfStars) < 0.000001) {
            "Personal rating must be between 0.5 and 5.0 in 0.5 increments"
        }
        return halfStars
    }

    fun toStars(halfStars: Int): Double {
        requireValid(halfStars)
        return halfStars / 2.0
    }
}
