package com.workoutlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Red & black for the phone's dark mode.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF3B30),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3D0B0B),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFF8A80),
    onSecondary = Color(0xFF2A0000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFFF6E6E),
)

// Red & white for the phone's light mode.
private val LightColors = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE3E0),
    onPrimaryContainer = Color(0xFF5C0000),
    secondary = Color(0xFFB71C1C),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141414),
    surfaceVariant = Color(0xFFF3EDED),
    onSurfaceVariant = Color(0xFF5E5555),
    surfaceContainer = Color(0xFFF8F3F3),
    surfaceContainerHigh = Color(0xFFF2ECEC),
    surfaceContainerHighest = Color(0xFFEBE4E4),
    outline = Color(0xFFBDB3B3),
    outlineVariant = Color(0xFFE3DADA),
    error = Color(0xFFB3261E),
)

private val LocalHint = staticCompositionLocalOf { Color.Gray }

/** Light-colored text for hints such as the target rep range and previous-session values. */
val Hint: Color
    @Composable @ReadOnlyComposable
    get() = LocalHint.current

@Composable
fun BeastsJourneyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalHint provides if (dark) Color(0xFF6E6E6E) else Color(0xFFAFA5A5)) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
    }
}
