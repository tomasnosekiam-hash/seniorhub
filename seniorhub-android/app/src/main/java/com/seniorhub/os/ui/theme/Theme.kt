package com.seniorhub.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF000000)
private val OnBackground = Color(0xFFFFFFFF)
private val Accent = Color(0xFFFFFF00)

private val SeniorHubColors = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    secondary = Accent,
    onSecondary = Background,
    background = Background,
    onBackground = OnBackground,
    surface = Background,
    onSurface = OnBackground,
)

@Composable
fun SeniorHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeniorHubColors,
        content = content,
    )
}
