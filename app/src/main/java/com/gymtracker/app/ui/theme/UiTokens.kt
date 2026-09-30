package com.gymtracker.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.gymtracker.app.data.model.ExerciseCategory

object UiTokens {
    val BgTop = Color(0xFF0E1116)
    val BgBottom = Color(0xFF0A0C11)
    val Surface = Color(0xFF141A22)
    val SurfaceElevated = Color(0xFF1B222C)
    val SurfaceMuted = Color(0xFF232C38)
    val GlassBorder = Color(0xFF2A3446)

    // Category colors
    val Trbusnjaci = Color(0xFFFF6B6B)
    val Ledja = Color(0xFF4ECDC4)
    val Biceps = Color(0xFFFFE66D)
    val Kardio = Color(0xFFFF8C42)
    val Grudi = Color(0xFF95E1D3)
    val Noge = Color(0xFFA8E6CF)
    val Ramena = Color(0xFF7B68EE)
    val Triceps = Color(0xFFDDA0DD)

    val Accent = Color(0xFF3EC8A8)
    val AccentAlt = Color(0xFF6EA8FF)
    val Danger = Color(0xFFF06F6F)
    val Warning = Color(0xFFF3B25B)
    val Success = Color(0xFF4CAF50)

    // Exercise type colors
    val Compound = Color(0xFF4ECDC4)    // Teal - multi-joint
    val Isolation = Color(0xFF98A2B3)   // Muted gray - single-joint

    val TextPrimary = Color(0xFFE9EDF5)
    val TextSecondary = Color(0xFFC7CEDB)
    val TextMuted = Color(0xFF98A2B3)
}

fun categoryColor(category: ExerciseCategory): Color {
    return when (category) {
        ExerciseCategory.TRBUSNJACI -> UiTokens.Trbusnjaci
        ExerciseCategory.LEDJA -> UiTokens.Ledja
        ExerciseCategory.BICEPS -> UiTokens.Biceps
        ExerciseCategory.KARDIO -> UiTokens.Kardio
        ExerciseCategory.GRUDI -> UiTokens.Grudi
        ExerciseCategory.NOGE -> UiTokens.Noge
        ExerciseCategory.RAMENA -> UiTokens.Ramena
        ExerciseCategory.TRICEPS -> UiTokens.Triceps
    }
}
