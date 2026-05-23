package com.seniorhub.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SeniorHubColorScheme = darkColorScheme(
    background = SeniorHubDesign.DashboardBackground,
    onBackground = SeniorHubDesign.AccentGold,
    surface = SeniorHubDesign.DashboardBackground,
    onSurface = SeniorHubDesign.AccentGold,
    surfaceContainer = SeniorHubDesign.WeatherSurface,
    onSurfaceVariant = SeniorHubDesign.MenuInactive,
    primary = SeniorHubDesign.AccentGold,
    onPrimary = SeniorHubDesign.Black,
    secondary = SeniorHubDesign.WeatherSurface,
    onSecondary = SeniorHubDesign.WeatherText,
    tertiary = SeniorHubDesign.WeatherSun,
    outline = SeniorHubDesign.MenuInactive,
    outlineVariant = SeniorHubDesign.MessageReadSurface,
)

@Composable
fun SeniorHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SeniorHubColorScheme,
        typography = SeniorHubTypography,
        shapes = SeniorHubShapes,
        content = content,
    )
}
