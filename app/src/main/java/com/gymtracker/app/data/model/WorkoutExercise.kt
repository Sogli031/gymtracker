package com.gymtracker.app.data.model

import java.util.UUID

data class WorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val exerciseName: String,
    val category: ExerciseCategory,
    val sets: List<ExerciseSet> = listOf(ExerciseSet())
) {
    fun addSet(): WorkoutExercise {
        return copy(sets = sets + ExerciseSet())
    }

    fun updateSet(setId: String, kg: Double?, reps: Int?): WorkoutExercise {
        return copy(sets = sets.map {
            if (it.id == setId) it.copy(kg = kg, reps = reps) else it
        })
    }

    fun removeSet(setId: String): WorkoutExercise {
        if (sets.size <= 1) return this
        return copy(sets = sets.filter { it.id != setId })
    }

    fun copyFresh(): WorkoutExercise {
        return WorkoutExercise(
            exerciseName = exerciseName,
            category = category
        )
    }

    /**
     * Get the best set by estimated 1RM
     */
    val bestSet: ExerciseSet?
        get() = sets.filter { it.isComplete }.maxByOrNull { it.estimated1RM ?: 0.0 }

    /**
     * Get best estimated 1RM for this exercise
     */
    val best1RM: Double?
        get() = bestSet?.estimated1RM
}
