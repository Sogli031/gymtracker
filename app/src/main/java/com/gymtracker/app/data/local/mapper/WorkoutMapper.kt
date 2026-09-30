package com.gymtracker.app.data.local.mapper

import com.gymtracker.app.data.local.entity.ExerciseSetEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.model.Config
import com.gymtracker.app.data.model.ExerciseCategory
import com.gymtracker.app.data.model.ExerciseSet
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.data.model.WorkoutExercise
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object WorkoutMapper {

    /**
     * Domen → Entity trostruko mapiranje.
     * Vraća Triple(WorkoutEntity, lista WorkoutExerciseEntity, lista ExerciseSetEntity).
     */
    fun toEntities(workout: Workout): Triple<WorkoutEntity, List<WorkoutExerciseEntity>, List<ExerciseSetEntity>> {
        val workoutEntity = WorkoutEntity(
            id = workout.id,
            name = workout.name,
            date = workout.date.format(Config.storageFormatter),
            time = workout.time.format(Config.storageTimeFormatter),
            endTime = workout.endTime?.format(Config.storageTimeFormatter),
            createdAt = workout.createdAt.toString()
        )

        val exerciseEntities = mutableListOf<WorkoutExerciseEntity>()
        val setEntities = mutableListOf<ExerciseSetEntity>()

        workout.exercises.forEachIndexed { exIndex, exercise ->
            exerciseEntities.add(
                WorkoutExerciseEntity(
                    id = exercise.id,
                    workoutId = workout.id,
                    exerciseName = exercise.exerciseName,
                    category = exercise.category.name,
                    orderIndex = exIndex
                )
            )
            exercise.sets.forEachIndexed { setIndex, set ->
                setEntities.add(
                    ExerciseSetEntity(
                        id = set.id,
                        exerciseId = exercise.id,
                        kg = set.kg,
                        reps = set.reps,
                        orderIndex = setIndex
                    )
                )
            }
        }

        return Triple(workoutEntity, exerciseEntities, setEntities)
    }

    /**
     * Entity → Domen batch mapiranje.
     * Sastavlja kompletne Workout objekte iz tri flat liste.
     */
    fun toDomainList(
        workouts: List<WorkoutEntity>,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<ExerciseSetEntity>
    ): List<Workout> {
        val setsByExerciseId = sets.groupBy { it.exerciseId }
        val exercisesByWorkoutId = exercises.groupBy { it.workoutId }

        return workouts.map { we ->
            val wxEntities = exercisesByWorkoutId[we.id].orEmpty().sortedBy { it.orderIndex }
            Workout(
                id = we.id,
                name = we.name,
                date = LocalDate.parse(we.date, Config.storageFormatter),
                time = LocalTime.parse(we.time, Config.storageTimeFormatter),
                endTime = we.endTime?.let { LocalTime.parse(it, Config.storageTimeFormatter) },
                exercises = wxEntities.map { wxe ->
                    val sEntities = setsByExerciseId[wxe.id].orEmpty().sortedBy { it.orderIndex }
                    WorkoutExercise(
                        id = wxe.id,
                        exerciseName = wxe.exerciseName,
                        // Bezbedno parsiranje: nepoznata/oštećena kategorija (npr. iz starijeg
                        // ili korumpiranog cloud backup-a) ne sme da obori ceo collector.
                        category = ExerciseCategory.fromName(wxe.category) ?: ExerciseCategory.KARDIO,
                        sets = sEntities.map { se ->
                            ExerciseSet(id = se.id, kg = se.kg, reps = se.reps)
                        }.ifEmpty { listOf(ExerciseSet()) }
                    )
                },
                createdAt = LocalDateTime.parse(we.createdAt)
            )
        }
    }
}
