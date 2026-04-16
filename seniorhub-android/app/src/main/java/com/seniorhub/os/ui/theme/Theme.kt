package com.seniorhub.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF000000)
private val SurfaceLow = Color(0xFF121212)
private val SurfaceContainer = Color(0xFF1C1C1C)
private val SurfaceContainerHigh = Color(0xFF2A2A2A)
private val OnSurfaceVariant = Color(0xFFC7C7C7)
private val Outline = Color(0xFF3F3F3F)
private val Accent = Color(0xFFF5E100)
private val OnAccent = Color(0xFF1C1B00)

private val SeniorHubColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFF4A4500),
    onPrimaryContainer = Color(0xFFFFF6A3),
    secondary = Color(0xFFD4CE00),
    onSecondary = Color(0xFF1C1B00),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF2D1600),
    tertiaryContainer = Color(0xFF5C3800),
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = Background,
    onBackground = Color(0xFFF6F6F6),
    surface = SurfaceLow,
    onSurface = Color(0xFFF6F6F6),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = Background,
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF363636),
    outline = Outline,
    outlineVariant = Color(0xFF2A2A2A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A22),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun SeniorHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeniorHubColors,
        typography = SeniorHubTypography,
        shapes = SeniorHubShapes,
        content = content,
    )
}
