package com.softvinax.lantern.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for Lantern's palette. Screens must never reference a
 * [Color] literal directly; they read from [LanternTheme.colors].
 *
 * Dark theme only, as specified by the prototype.
 */
object LanternPalette {
    val Midnight = Color(0xFF060A18)
    val Surface = Color(0xFF0C1326)
    val SurfaceElevated = Color(0xFF111A30)
    val SurfaceVariant = Color(0xFF18223B)
    val Border = Color(0xFF2A3550)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFA9B6CC)

    val Amber = Color(0xFFFBBF24)
    val Orange = Color(0xFFF97316)
    val Teal = Color(0xFF2DD4BF)
    val Blue = Color(0xFF60A5FA)
    val Indigo = Color(0xFF818CF8)
    val Coral = Color(0xFFFB7185)

    /** Midnight text on amber/orange has >= 6:1 contrast. */
    val OnAccent = Midnight
}

@Immutable
data class LanternColors(
    val background: Color = LanternPalette.Midnight,
    val surface: Color = LanternPalette.Surface,
    val surfaceElevated: Color = LanternPalette.SurfaceElevated,
    val surfaceVariant: Color = LanternPalette.SurfaceVariant,
    val border: Color = LanternPalette.Border,
    val textPrimary: Color = LanternPalette.TextPrimary,
    val textSecondary: Color = LanternPalette.TextSecondary,
    val amber: Color = LanternPalette.Amber,
    val orange: Color = LanternPalette.Orange,
    val onAccent: Color = LanternPalette.OnAccent,
    val teal: Color = LanternPalette.Teal,
    val blue: Color = LanternPalette.Blue,
    val indigo: Color = LanternPalette.Indigo,
    val coral: Color = LanternPalette.Coral,
    val neutralBadge: Color = LanternPalette.SurfaceVariant,
    val neutralBadgeText: Color = LanternPalette.TextSecondary,
    val primaryGradient: List<Color> = listOf(LanternPalette.Amber, LanternPalette.Orange),
    val cloudGradient: List<Color> = listOf(LanternPalette.Blue, LanternPalette.Indigo),
    val onDeviceTint: Color = LanternPalette.Teal.copy(alpha = 0.14f),
    val cloudTint: Color = LanternPalette.Blue.copy(alpha = 0.14f),
    val neutralTint: Color = LanternPalette.SurfaceVariant.copy(alpha = 0.72f),
    val coralTint: Color = LanternPalette.Coral.copy(alpha = 0.14f),
    val amberTint: Color = LanternPalette.Amber.copy(alpha = 0.14f),
    val tealTint: Color = LanternPalette.Teal.copy(alpha = 0.14f),
    val blueTint: Color = LanternPalette.Blue.copy(alpha = 0.14f),
)

val LocalLanternColors = staticCompositionLocalOf { LanternColors() }

object LanternTheme {
    val colors: LanternColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLanternColors.current
}
