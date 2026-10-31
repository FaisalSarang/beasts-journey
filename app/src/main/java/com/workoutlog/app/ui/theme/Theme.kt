package com.workoutlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Lime = Color(0xFFC6F432)
val Hint = Color(0xFF6B7380)

private val Colors = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF151A00),
    primaryContainer = Color(0xFF2E3A0A),
    onPrimaryContainer = Color(0xFFE3FF9A),
    secondary = Color(0xFF9FB4C7),
    onSecondary = Color(0xFF0E1B26),
    background = Color(0xFF121416),
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF121416),
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF22262B),
    onSurfaceVariant = Color(0xFFAEB4BC),
    surfaceContainer = Color(0xFF1B1F24),
    surfaceContainerHigh = Color(0xFF23282E),
    surfaceContainerHighest = Color(0xFF2B3138),
    outline = Color(0xFF3A4048),
    outlineVariant = Color(0xFF2C3137),
    error = Color(0xFFFF6B6B),
)

@Composable
fun WorkoutLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
