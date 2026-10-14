package com.sergo379.gamedex.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val RatingGold = Color(0xFFFFC857)

@Composable
fun PersonalRatingStars(
    valueHalfStars: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = valueHalfStars ?: 0
    Row(
        modifier = modifier
            .testTag("personal-rating-stars")
            .semantics {
                stateDescription = valueHalfStars?.let { "Моя оценка ${ratingText(it)} из 5" }
                    ?: "Оценка не выставлена"
            },
    ) {
        repeat(5) { starIndex ->
            val targetFill = ((selected - starIndex * 2).coerceIn(0, 2)) / 2f
            val fill by animateFloatAsState(targetFill, tween(180), label = "rating-star-fill")
            Box(Modifier.size(52.dp)) {
                Canvas(Modifier.matchParentSize()) {
                    val path = starPath(size.width, size.height)
                    drawPath(path, RatingGold.copy(alpha = 0.13f))
                    clipRect(right = size.width * fill) { drawPath(path, RatingGold) }
                    drawPath(path, RatingGold.copy(alpha = 0.9f), style = Stroke(width = 2.dp.toPx()))
                }
                Row(Modifier.matchParentSize()) {
                    repeat(2) { halfIndex ->
                        val halfStars = starIndex * 2 + halfIndex + 1
                        val description = "Установить оценку ${ratingText(halfStars)} из 5"
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onValueChange(halfStars) }
                                .clearAndSetSemantics {
                                    role = Role.Button
                                    contentDescription = description
                                    onClick(description) {
                                        onValueChange(halfStars)
                                        true
                                    }
                                }
                                .testTag("rating-$halfStars"),
                        )
                    }
                }
            }
        }
    }
}

fun ratingText(halfStars: Int): String = String.format(Locale("ru", "RU"), "%.1f", halfStars / 2.0)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.starPath(width: Float, height: Float): Path {
    val center = Offset(width / 2f, height / 2f)
    val outer = minOf(width, height) * 0.43f
    val inner = outer * 0.46f
    return Path().apply {
        repeat(10) { point ->
            val radius = if (point % 2 == 0) outer else inner
            val angle = -PI / 2 + point * PI / 5
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (point == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}
