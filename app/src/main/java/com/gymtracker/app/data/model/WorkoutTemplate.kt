package com.gymtracker.app.data.model

/** Vrsta novog treninga koja se bira na „+“. */
enum class WorkoutTemplate(val label: String, val description: String) {
    VOLUME("Volume", "Trenutni trening celog tela (2–3 serije)"),
    HIT("HIT", "Sve vežbe po 1 serija do otkaza");

    companion object {
        fun fromRoute(value: String?): WorkoutTemplate =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: VOLUME
    }
}

private const val CHEST_PRESS = "Chest Press Machine"
private const val BENCH_PRESS = "Bench Press"
private const val LAT_PULLDOWN = "Lat Pulldown"
private const val CABLE_ROW = "Cable Row"

private fun templateExercise(name: String, category: ExerciseCategory, sets: Int) =
    WorkoutExercise(exerciseName = name, category = category, sets = List(sets) { ExerciseSet() })

/**
 * Vežbe novog treninga za izabrani šablon.
 *
 * Volume smenjuje grudi (Chest Press ↔ Bench Press) i leđa (Lat Pulldown ↔ Cable Row) prema poslednjem
 * odrađenom treningu koji ima tačno jednu vežbu iz para; HIT ima obe, pa ne utiče na smenu.
 */
fun templateExercises(template: WorkoutTemplate, history: List<Workout>): List<WorkoutExercise> =
    when (template) {
        WorkoutTemplate.HIT -> listOf(
            templateExercise(CHEST_PRESS, ExerciseCategory.GRUDI, 1),
            templateExercise(LAT_PULLDOWN, ExerciseCategory.LEDJA, 1),
            templateExercise(BENCH_PRESS, ExerciseCategory.GRUDI, 1),
            templateExercise(CABLE_ROW, ExerciseCategory.LEDJA, 1),
            templateExercise("Leg Press", ExerciseCategory.NOGE, 1),
            templateExercise("Cable Overhead Extension", ExerciseCategory.TRICEPS, 1),
            templateExercise("Dumbbell Curl", ExerciseCategory.BICEPS, 1)
        )
        WorkoutTemplate.VOLUME -> {
            val chest = if (lastDoneOf(history, CHEST_PRESS, BENCH_PRESS) == CHEST_PRESS) BENCH_PRESS else CHEST_PRESS
            val back = if (lastDoneOf(history, LAT_PULLDOWN, CABLE_ROW) == LAT_PULLDOWN) CABLE_ROW else LAT_PULLDOWN
            listOf(
                templateExercise(chest, ExerciseCategory.GRUDI, 3),
                templateExercise(back, ExerciseCategory.LEDJA, 3),
                templateExercise("Leg Press", ExerciseCategory.NOGE, 3),
                templateExercise("Cable Overhead Extension", ExerciseCategory.TRICEPS, 2),
                templateExercise("Dumbbell Curl", ExerciseCategory.BICEPS, 2),
                templateExercise("Lateral Raise", ExerciseCategory.RAMENA, 2)
            )
        }
    }

/** Koja od dve vežbe je odrađena (bar jedna puna serija) na poslednjem treningu koji ima samo jednu od njih. */
private fun lastDoneOf(history: List<Workout>, first: String, second: String): String? =
    history
        .sortedWith(compareByDescending<Workout> { it.date }.thenByDescending { it.time })
        .asSequence()
        .map { workout ->
            workout.exercises
                .filter { it.sets.any(ExerciseSet::isComplete) }
                .map { it.exerciseName }
                .filter { it.equals(first, ignoreCase = true) || it.equals(second, ignoreCase = true) }
                .map { if (it.equals(first, ignoreCase = true)) first else second }
                .toSet()
        }
        .firstOrNull { it.size == 1 }
        ?.single()
