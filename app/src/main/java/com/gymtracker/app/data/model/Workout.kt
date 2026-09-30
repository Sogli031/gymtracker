package com.gymtracker.app.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val date: LocalDate,
    val time: LocalTime,
    val endTime: LocalTime? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    val displayDate: String
        get() = date.format(Config.displayFormatter)

    val displayTime: String
        get() = time.format(Config.timeFormatter)

    val displayEndTime: String?
        get() = endTime?.format(Config.timeFormatter)

    val displayTimeRange: String
        get() = if (endTime != null) {
            "$displayTime - ${endTime.format(Config.timeFormatter)}"
        } else {
            displayTime
        }

    val displayDateTime: String
        get() = "$displayDate $displayTime"

    val displayDuration: String?
        get() {
            if (endTime == null) return null
            var minutes = java.time.Duration.between(time, endTime).toMinutes().toInt()
            if (minutes < 0) minutes += 24 * 60  // prelaz preko ponoći (endTime nema datum)
            val h = minutes / 60
            val m = minutes % 60
            return if (h > 0) "${h}h ${m}min" else "${m}min"
        }

    val displayName: String
        get() = name ?: suggestedName ?: categorySummary.ifEmpty { displayDate }

    /**
     * Automatically suggests a workout name based on exercises performed.
     * Common gym workout splits are detected.
     */
    val suggestedName: String?
        get() {
            if (exercises.isEmpty()) return null

            val categories = exercises.map { it.category }.toSet()
            val hasChest = ExerciseCategory.GRUDI in categories
            val hasBack = ExerciseCategory.LEDJA in categories
            val hasLegs = ExerciseCategory.NOGE in categories
            val hasShoulders = ExerciseCategory.RAMENA in categories
            val hasBiceps = ExerciseCategory.BICEPS in categories
            val hasTriceps = ExerciseCategory.TRICEPS in categories
            val hasAbs = ExerciseCategory.TRBUSNJACI in categories
            val hasCardio = ExerciseCategory.KARDIO in categories

            // Count major muscle groups
            val majorGroups = listOf(hasChest, hasBack, hasLegs, hasShoulders).count { it }

            // Redosled je bitan: specifičnije kombinacije MORAJU biti pre opštijih,
            // inače opštija grana "proguta" specifičniju (npr. "Gornji deo tela"
            // je širi od "Grudi i leđa", pa mora ići posle nje).
            return when {
                // Trening jedne grupe
                categories.size == 1 -> when (categories.first()) {
                    ExerciseCategory.GRUDI -> "Dan grudi"
                    ExerciseCategory.LEDJA -> "Dan leđa"
                    ExerciseCategory.NOGE -> "Dan nogu"
                    ExerciseCategory.RAMENA -> "Dan ramena"
                    ExerciseCategory.BICEPS -> "Biceps"
                    ExerciseCategory.TRICEPS -> "Triceps"
                    ExerciseCategory.TRBUSNJACI -> "Trbušnjaci"
                    ExerciseCategory.KARDIO -> "Kardio"
                }

                // Push (Grudi + Ramena + Triceps)
                hasChest && hasShoulders && hasTriceps && !hasBack && !hasLegs && !hasBiceps -> "Push dan"

                // Pull (Leđa + Biceps)
                hasBack && hasBiceps && !hasChest && !hasLegs && !hasTriceps -> "Pull dan"

                // Ruke (Biceps + Triceps)
                hasBiceps && hasTriceps && !hasChest && !hasBack && !hasLegs -> "Dan ruku"

                // Grudi i triceps
                hasChest && hasTriceps && !hasBack && !hasLegs -> "Grudi i triceps"

                // Leđa i biceps
                hasBack && hasBiceps && !hasChest && !hasLegs -> "Leđa i biceps"

                // Grudi i leđa (bez ramena) — mora pre "Gornji deo tela"
                hasChest && hasBack && !hasLegs && !hasShoulders -> "Grudi i leđa"

                // Gornji deo tela (Grudi + Leđa + ramena/ruke)
                hasChest && hasBack && !hasLegs -> "Gornji deo tela"

                // Donji deo tela (noge + trbušnjaci/kardio) — mora pre "Dan nogu"
                hasLegs && (hasAbs || hasCardio) && !hasChest && !hasBack -> "Donji deo tela"

                // Noge (eventualno sa ničim drugim velikim)
                hasLegs && !hasChest && !hasBack && !hasShoulders -> "Dan nogu"

                // Celo telo (3+ velikih grupa)
                majorGroups >= 3 -> "Celo telo"

                // Kardio fokus sa malo snage
                hasCardio && categories.size <= 2 -> "Kardio"

                else -> null // Vrati se na rezime kategorija
            }
        }

    val totalExercises: Int
        get() = exercises.size

    val totalSets: Int
        get() = exercises.sumOf { it.sets.size }

    val categorySummary: String
        get() {
            val categories = exercises.map { it.category }.distinct()
            return when {
                categories.isEmpty() -> ""
                categories.size <= 3 -> categories.joinToString(", ") { it.displayName }
                else -> "${categories.take(2).joinToString(", ") { it.displayName }} +${categories.size - 2}"
            }
        }

    fun addExercise(exercise: WorkoutExercise): Workout {
        return copy(exercises = exercises + exercise)
    }

    fun updateExercise(exerciseId: String, updatedExercise: WorkoutExercise): Workout {
        return copy(exercises = exercises.map {
            if (it.id == exerciseId) updatedExercise else it
        })
    }

    fun removeExercise(exerciseId: String): Workout {
        return copy(exercises = exercises.filter { it.id != exerciseId })
    }
}
