package com.gymtracker.app.data.model

import java.time.format.DateTimeFormatter

object Config {
    const val DATA_FILE = "workout_data.json"
    const val CUSTOM_EXERCISES_FILE = "custom_exercises.json"
    const val BODY_METRICS_FILE = "body_metrics.json"
    const val CYCLES_FILE = "cycles.json"
    const val MACHINE_SETTINGS_FILE = "machine_settings.json"

    val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val storageFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val storageTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    // Nutrition konstante
    /** Kalorijski surplus za rast mišića (kcal) */
    const val CALORIE_SURPLUS = 400
    /** Protein po kg telesne mase (g/kg) */
    const val PROTEIN_PER_KG = 2.0f

    // Brzycki formula konstante
    /** Maksimalan broj ponavljanja za pouzdanu Brzycki procenu */
    const val BRZYCKI_MAX_REPS = 30

    // Workout defaults
    /** Podrazumevano trajanje treninga u minutima ako nije unet kraj */
    const val DEFAULT_WORKOUT_DURATION_MINUTES = 30L

    // Makro distribucija (procenat ukupnih kalorija)
    const val MACRO_CARBS_RATIO = 0.60
    const val MACRO_PROTEIN_RATIO = 0.25
    const val MACRO_FAT_RATIO = 0.15

    // Kalorije po gramu makronutrijenta
    const val CALORIES_PER_GRAM_CARB = 4
    const val CALORIES_PER_GRAM_PROTEIN = 4
    const val CALORIES_PER_GRAM_FAT = 9
}
