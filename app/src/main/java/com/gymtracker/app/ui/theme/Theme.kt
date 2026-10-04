package com.gymtracker.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Kompletna M3 tamna šema (seed #3EC8A8). Sve uloge su popunjene da nijedna
// komponenta (meni, sheet, switch, chip) ne padne na podrazumevane ljubičaste boje.
// Kontrast proveren: onPrimary/primary 6.3:1, onSurfaceVariant/surface 8.9:1,
// outline/surfaceContainerHigh 4.2:1.
private val AppColorScheme = darkColorScheme(
    primary = UiTokens.Accent,
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005143),
    onPrimaryContainer = Color(0xFF7DF8D6),
    inversePrimary = Color(0xFF006B58),

    secondary = UiTokens.AccentAlt,
    onSecondary = Color(0xFF002F65),
    secondaryContainer = Color(0xFF26354A),
    onSecondaryContainer = Color(0xFFD6E3FF),

    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6A3C00),
    onTertiaryContainer = Color(0xFFFFDCBE),

    error = UiTokens.Danger,
    onError = Color(0xFF4A0A0A),
    errorContainer = Color(0xFF5C1A1A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = UiTokens.BgTop,
    onBackground = UiTokens.TextPrimary,
    surface = UiTokens.BgTop,
    onSurface = UiTokens.TextPrimary,
    surfaceVariant = UiTokens.SurfaceMuted,
    onSurfaceVariant = Color(0xFFA9B3C3),
    surfaceTint = UiTokens.Accent,
    inverseSurface = UiTokens.TextPrimary,
    inverseOnSurface = UiTokens.SurfaceElevated,

    surfaceDim = UiTokens.BgBottom,
    surfaceBright = Color(0xFF2E3542),
    surfaceContainerLowest = UiTokens.BgBottom,
    surfaceContainerLow = Color(0xFF11161D),
    surfaceContainer = UiTokens.Surface,
    surfaceContainerHigh = UiTokens.SurfaceElevated,
    surfaceContainerHighest = UiTokens.SurfaceMuted,

    outline = Color(0xFF77839A),
    outlineVariant = UiTokens.GlassBorder,
    scrim = Color.Black
)

// M3 baseline type scale (Roboto): Display 57/45/36, Headline 32/28/24,
// Title 22/16/14, Body 16/14/12, Label 14/12/11.
private val AppTypography = Typography()

// M3 shape scale; extraLarge = 28dp za dijaloge i bottom sheet-ove.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Kartice koriste M3 shape `medium` (12dp). */
val CardShape = AppShapes.medium

/** Max širina sadržaja na tabletu / landscape-u (M3: 840dp). */
val MaxContentWidth = 840.dp

@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
