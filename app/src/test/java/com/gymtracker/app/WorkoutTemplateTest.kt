package com.gymtracker.app

import com.gymtracker.app.data.model.ExerciseCategory
import com.gymtracker.app.data.model.ExerciseSet
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.data.model.WorkoutExercise
import com.gymtracker.app.data.model.WorkoutTemplate
import com.gymtracker.app.data.model.templateExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WorkoutTemplateTest {

    private fun done(name: String, category: ExerciseCategory) =
        WorkoutExercise(exerciseName = name, category = category, sets = listOf(ExerciseSet(kg = 50.0, reps = 8)))

    private fun workout(day: Int, vararg exercises: WorkoutExercise) =
        Workout(date = LocalDate.of(2026, 10, day), time = LocalTime.of(18, 0), exercises = exercises.toList())

    @Test
    fun `hit is seven exercises with one set each in fixed order`() {
        val hit = templateExercises(WorkoutTemplate.HIT, emptyList())
        assertEquals(
            listOf("Chest Press Machine", "Lat Pulldown", "Bench Press", "Cable Row", "Leg Press", "Cable Overhead Extension", "Dumbbell Curl"),
            hit.map { it.exerciseName }
        )
        assertTrue(hit.all { it.sets.size == 1 && !it.sets.single().isComplete })
    }

    @Test
    fun `volume keeps the current template and alternates chest and back`() {
        val first = templateExercises(WorkoutTemplate.VOLUME, emptyList())
        assertEquals(
            listOf("Chest Press Machine", "Lat Pulldown", "Leg Press", "Cable Overhead Extension", "Dumbbell Curl", "Lateral Raise"),
            first.map { it.exerciseName }
        )
        assertEquals(listOf(3, 3, 3, 2, 2, 2), first.map { it.sets.size })

        val afterVolume = listOf(
            workout(1, done("Chest Press Machine", ExerciseCategory.GRUDI), done("Lat Pulldown", ExerciseCategory.LEDJA))
        )
        val next = templateExercises(WorkoutTemplate.VOLUME, afterVolume).map { it.exerciseName }
        assertEquals("Bench Press", next[0])
        assertEquals("Cable Row", next[1])
    }

    @Test
    fun `hit workout does not break the volume alternation`() {
        val history = listOf(
            workout(1, done("Bench Press", ExerciseCategory.GRUDI), done("Cable Row", ExerciseCategory.LEDJA)),
            // Noviji HIT ima obe vežbe iz para: preskače se, odlučuje poslednji Volume.
            workout(
                3,
                done("Chest Press Machine", ExerciseCategory.GRUDI),
                done("Lat Pulldown", ExerciseCategory.LEDJA),
                done("Bench Press", ExerciseCategory.GRUDI),
                done("Cable Row", ExerciseCategory.LEDJA)
            )
        )
        val next = templateExercises(WorkoutTemplate.VOLUME, history).map { it.exerciseName }
        assertEquals("Chest Press Machine", next[0])
        assertEquals("Lat Pulldown", next[1])
    }

    @Test
    fun `route value maps to template and defaults to volume`() {
        assertEquals(WorkoutTemplate.HIT, WorkoutTemplate.fromRoute("HIT"))
        assertEquals(WorkoutTemplate.VOLUME, WorkoutTemplate.fromRoute(null))
        assertEquals(WorkoutTemplate.VOLUME, WorkoutTemplate.fromRoute("nepoznato"))
    }
}
