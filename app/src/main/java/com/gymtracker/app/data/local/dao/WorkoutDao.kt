package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gymtracker.app.data.local.entity.ExerciseSetEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // === Observation ===

    @Query("SELECT * FROM workouts ORDER BY date DESC, time DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    // === Batch učitavanje ===

    @Query("SELECT * FROM workouts ORDER BY date DESC, time DESC")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_exercises WHERE workout_id IN (:workoutIds) ORDER BY order_index")
    suspend fun getExercisesForWorkouts(workoutIds: List<String>): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM exercise_sets WHERE exercise_id IN (:exerciseIds) ORDER BY order_index")
    suspend fun getSetsForExercises(exerciseIds: List<String>): List<ExerciseSetEntity>

    // Bez IN liste — za backup cele baze (IN lista ima SQLite limit broja parametara).
    @Query("SELECT * FROM workout_exercises ORDER BY order_index")
    suspend fun getAllExercises(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM exercise_sets ORDER BY order_index")
    suspend fun getAllSets(): List<ExerciseSetEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): WorkoutEntity?

    // === Insert ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<ExerciseSetEntity>)

    // === Delete ===

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)

    @Query("DELETE FROM workout_exercises WHERE workout_id = :workoutId")
    suspend fun deleteExercisesForWorkout(workoutId: String)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllExercises()

    @Query("DELETE FROM exercise_sets")
    suspend fun deleteAllSets()

    // === Transakcione metode ===

    @Transaction
    suspend fun insertFullWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<ExerciseSetEntity>
    ) {
        insertWorkout(workout)
        if (exercises.isNotEmpty()) insertExercises(exercises)
        if (sets.isNotEmpty()) insertSets(sets)
    }

    @Transaction
    suspend fun deleteAll() {
        deleteAllSets()
        deleteAllExercises()
        deleteAllWorkouts()
    }

    @Transaction
    suspend fun updateFullWorkout(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<ExerciseSetEntity>
    ) {
        deleteExercisesForWorkout(workout.id)
        insertWorkout(workout)
        if (exercises.isNotEmpty()) insertExercises(exercises)
        if (sets.isNotEmpty()) insertSets(sets)
    }
}
