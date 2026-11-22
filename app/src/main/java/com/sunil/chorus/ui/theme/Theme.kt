package com.sunil.chorus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ChorusColors = darkColorScheme(
    primary = Amber,
    onPrimary = MidnightDeep,
    secondary = Gold,
    background = Midnight,
    onBackground = StarWhite,
    surface = NightCard,
    onSurface = StarWhite,
    surfaceVariant = NightCard,
    onSurfaceVariant = Mist,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCard,
    surfaceContainerLow = NightCard,
    outline = NightLine,
)

@Composable
fun ChorusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChorusColors,
        typography = Typography,
        content = content,
    )
}