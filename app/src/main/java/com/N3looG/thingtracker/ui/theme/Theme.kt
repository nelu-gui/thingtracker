package com.N3looG.thingtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TechDarkColorScheme = darkColorScheme(
    primary = TechPrimary,
    secondary = TechSecondary,
    tertiary = TechAccent,
    background = TechBackground,
    surface = TechSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TechTextPrimary,
    onSurface = TechTextPrimary
)

@Composable
fun ThingTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TechDarkColorScheme,
        typography = Typography,
        content = content
    )
}
