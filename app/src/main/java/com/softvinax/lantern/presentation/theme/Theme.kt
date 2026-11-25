package com.softvinax.lantern.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LanternColorScheme = darkColorScheme(
    primary = LanternPalette.Amber,
    onPrimary = LanternPalette.OnAccent,
    primaryContainer = LanternPalette.Amber.copy(alpha = 0.16f),
    onPrimaryContainer = LanternPalette.Amber,
    secondary = LanternPalette.Teal,
    onSecondary = LanternPalette.OnAccent,
    secondaryContainer = LanternPalette.Teal.copy(alpha = 0.16f),
    onSecondaryContainer = LanternPalette.Teal,
    tertiary = LanternPalette.Blue,
    onTertiary = LanternPalette.OnAccent,
    tertiaryContainer = LanternPalette.Blue.copy(alpha = 0.16f),
    onTertiaryContainer = LanternPalette.Blue,
    error = LanternPalette.Coral,
    onError = LanternPalette.OnAccent,
    errorContainer = LanternPalette.Coral.copy(alpha = 0.16f),
    onErrorContainer = LanternPalette.Coral,
    background = LanternPalette.Midnight,
    onBackground = LanternPalette.TextPrimary,
    surface = LanternPalette.Surface,
    onSurface = LanternPalette.TextPrimary,
    surfaceVariant = LanternPalette.SurfaceVariant,
    onSurfaceVariant = LanternPalette.TextSecondary,
    outline = LanternPalette.Border,
    outlineVariant = LanternPalette.Border.copy(alpha = 0.6f),
    scrim = Color.Black.copy(alpha = 0.68f),
    inverseSurface = LanternPalette.TextPrimary,
    inverseOnSurface = LanternPalette.Midnight,
    inversePrimary = LanternPalette.Orange,
)

private val LanternShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LanternTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLanternColors provides LanternColors()) {
        MaterialTheme(
            colorScheme = LanternColorScheme,
            typography = LanternTypography,
            shapes = LanternShapes,
            content = content,
        )
    }
}
