package com.sergo379.gamedex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0B0D18)
val Surface = Color(0xFF141827)
val ElevatedSurface = Color(0xFF1C2235)
val Violet = Color(0xFF8B5CF6)
val Cyan = Color(0xFF22D3EE)
val Pink = Color(0xFFEC4899)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val GameDexColors = darkColorScheme(
    primary = Violet,
    secondary = Cyan,
    tertiary = Pink,
    background = Background,
    surface = Surface,
    surfaceVariant = ElevatedSurface,
    onPrimary = Color.White,
    onSecondary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = Color(0xFFFF6B81),
)

@Composable
fun GameDexTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GameDexColors, content = content)
}

