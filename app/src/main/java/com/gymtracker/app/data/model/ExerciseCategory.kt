package com.gymtracker.app.data.model

enum class ExerciseCategory(val displayName: String) {
    RAMENA("Ramena"),
    GRUDI("Grudi"),
    LEDJA("Leđa"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    TRBUSNJACI("Trbušnjaci"),
    NOGE("Noge"),
    KARDIO("Kardio");

    companion object {
        fun fromName(name: String): ExerciseCategory? {
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true)
            }
        }
    }
}
