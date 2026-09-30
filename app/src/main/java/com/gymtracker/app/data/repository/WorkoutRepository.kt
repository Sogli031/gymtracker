package com.gymtracker.app.data.repository

import android.app.Application
import android.util.Log
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.mapper.WorkoutMapper
import com.gymtracker.app.data.model.Config
import com.gymtracker.app.data.model.ExerciseSet
import com.gymtracker.app.data.model.Workout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val TAG = "WorkoutRepository"

class WorkoutRepository(private val app: Application, private val db: GymTrackerDatabase) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workoutDao = db.workoutDao()

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    init {
        ioScope.launch {
            workoutDao.observeAll().collect { workoutEntities ->
                // Ceo blok je zaštićen: ako mapiranje ili DB čitanje baci grešku,
                // _isLoaded i dalje postaje true (UI ne visi na loading-u zauvek).
                try {
                    if (workoutEntities.isEmpty()) {
                        _workouts.value = emptyList()
                        return@collect
                    }
                    val workoutIds = workoutEntities.map { it.id }
                    val allExercises = workoutDao.getExercisesForWorkouts(workoutIds)
                    val exerciseIds = allExercises.map { it.id }
                    val allSets = if (exerciseIds.isNotEmpty()) workoutDao.getSetsForExercises(exerciseIds) else emptyList()
                    _workouts.value = WorkoutMapper.toDomainList(workoutEntities, allExercises, allSets)
                } catch (e: Exception) {
                    Log.e(TAG, "Greška pri učitavanju treninga iz baze", e)
                    _loadError.value = "Greška pri učitavanju: ${e.message}"
                } finally {
                    _isLoaded.value = true
                }
            }
        }
    }

    /**
     * Poziva se posle zamene podataka iz backup-a.
     * Sami podaci stižu reaktivno kroz Room Flow.
     */
    fun clearCaches() {
        _loadError.value = null
    }

    fun add(workout: Workout) {
        ioScope.launch {
            val (we, exes, sets) = WorkoutMapper.toEntities(workout)
            workoutDao.insertFullWorkout(we, exes, sets)
        }
    }

    fun update(workout: Workout) {
        ioScope.launch {
            val (we, exes, sets) = WorkoutMapper.toEntities(workout)
            workoutDao.updateFullWorkout(we, exes, sets)
        }
    }

    suspend fun addSync(workout: Workout) {
        val (we, exes, sets) = WorkoutMapper.toEntities(workout)
        workoutDao.insertFullWorkout(we, exes, sets)
    }

    suspend fun updateSync(workout: Workout) {
        val (we, exes, sets) = WorkoutMapper.toEntities(workout)
        workoutDao.updateFullWorkout(we, exes, sets)
    }

    fun delete(id: String) {
        ioScope.launch {
            workoutDao.deleteWorkout(id)
        }
    }

    fun get(id: String): Workout? = _workouts.value.firstOrNull { it.id == id }

    /**
     * Get the last recorded set for a specific exercise name.
     * Used for smart placeholders.
     */
    fun getLastSetForExercise(exerciseName: String): ExerciseSet? {
        for (workout in _workouts.value) {
            for (exercise in workout.exercises) {
                if (exercise.exerciseName.equals(exerciseName, ignoreCase = true)) {
                    val completeSets = exercise.sets.filter { it.isComplete }
                    if (completeSets.isNotEmpty()) {
                        return completeSets.last()
                    }
                }
            }
        }
        return null
    }

    /**
     * Get all sets from the last workout for a specific exercise.
     * Returns list of sets in order (set 1, set 2, etc.) for per-set placeholders.
     * @param excludeWorkoutId If provided, skips this workout (useful when editing to show previous data).
     */
    fun getLastSetsForExercise(exerciseName: String, excludeWorkoutId: String? = null): List<ExerciseSet> {
        for (workout in _workouts.value) {
            if (workout.id == excludeWorkoutId) continue
            for (exercise in workout.exercises) {
                if (exercise.exerciseName.equals(exerciseName, ignoreCase = true)) {
                    val completeSets = exercise.sets.filter { it.isComplete }
                    // Preskoči trening gde je vežba dodata ali bez ijednog kompletnog seta
                    if (completeSets.isNotEmpty()) return completeSets
                }
            }
        }
        return emptyList()
    }

    /**
     * Get sets from the workout chronologically BEFORE the given workout.
     * Used for 1RM comparison when editing past workouts.
     * Workouts are sorted descending (newest first), so "before" means later index.
     */
    fun getSetsForExerciseBefore(exerciseName: String, beforeWorkoutId: String): List<ExerciseSet> {
        val workoutList = _workouts.value
        val refIndex = workoutList.indexOfFirst { it.id == beforeWorkoutId }
        if (refIndex < 0) return emptyList()

        for (i in (refIndex + 1) until workoutList.size) {
            for (exercise in workoutList[i].exercises) {
                if (exercise.exerciseName.equals(exerciseName, ignoreCase = true)) {
                    val completeSets = exercise.sets.filter { it.isComplete }
                    if (completeSets.isNotEmpty()) return completeSets
                }
            }
        }
        return emptyList()
    }

    /**
     * Get the first recorded set for a specific exercise (oldest workout).
     */
    fun getFirstSetForExercise(exerciseName: String): ExerciseSet? {
        // Workouts are sorted by date descending, so we need to go from the end
        for (workout in _workouts.value.reversed()) {
            for (exercise in workout.exercises) {
                if (exercise.exerciseName.equals(exerciseName, ignoreCase = true)) {
                    val completeSets = exercise.sets.filter { it.isComplete }
                    if (completeSets.isNotEmpty()) {
                        return completeSets.first()
                    }
                }
            }
        }
        return null
    }

    /**
     * Get progress data for all exercises that have been performed more than once.
     */
    fun getExerciseProgress(): List<ExerciseProgress> {
        // Store: exerciseName -> list of (bestSet, date)
        data class ExerciseRecord(
            val set: ExerciseSet,
            val date: java.time.LocalDate
        )
        val exerciseData = mutableMapOf<String, MutableList<ExerciseRecord>>()

        // Collect all exercise data (workouts are sorted by date descending)
        for (workout in _workouts.value) {
            for (exercise in workout.exercises) {
                val completeSets = exercise.sets.filter { it.isComplete }
                if (completeSets.isNotEmpty()) {
                    // Biraj najbolji set po procenjenom 1RM — isti metrik kojim se
                    // kasnije prikazuje i računa napredak (ranije je biran po volumenu
                    // kg×reps, što je davalo nekonzistentne rezultate).
                    val bestSet = completeSets.maxByOrNull { it.estimated1RM ?: 0.0 }
                    if (bestSet != null) {
                        exerciseData.getOrPut(exercise.exerciseName) { mutableListOf() }
                            .add(ExerciseRecord(bestSet, workout.date))
                    }
                }
            }
        }

        // Calculate progress for exercises with at least 2 records
        return exerciseData
            .filter { it.value.size >= 2 }
            .map { (name, records) ->
                val sortedRecords = records.sortedBy { it.date } // Sort by date ascending
                val first = sortedRecords.first()
                val last = sortedRecords.last()
                ExerciseProgress(
                    exerciseName = name,
                    firstKg = first.set.kg,
                    firstReps = first.set.reps,
                    lastKg = last.set.kg,
                    lastReps = last.set.reps,
                    workoutCount = records.size
                )
            }
            .sortedByDescending { it.progressPercent }
    }

}

data class ExerciseProgress(
    val exerciseName: String,
    val firstKg: Double?,
    val firstReps: Int?,
    val lastKg: Double?,
    val lastReps: Int?,
    val workoutCount: Int
) {
    /**
     * Calculate estimated 1RM using Brzycki formula (R² = 0.99):
     * 1RM = kg / (1.0278 - 0.0278 × reps)
     */
    private fun calculateBrzycki1RM(kg: Double?, reps: Int?): Double? {
        if (kg == null || reps == null || reps == 0) return null
        if (reps == 1) return kg
        if (reps > Config.BRZYCKI_MAX_REPS) return null
        val denominator = 1.0278 - 0.0278 * reps
        return if (denominator > 0) kg / denominator else null
    }

    val first1RM: Double?
        get() = calculateBrzycki1RM(firstKg, firstReps)

    val last1RM: Double?
        get() = calculateBrzycki1RM(lastKg, lastReps)

    val oneRMChange: Double?
        get() {
            val first = first1RM
            val last = last1RM
            return if (first != null && last != null) last - first else null
        }

    /**
     * Progress calculation using Brzycki 1RM.
     */
    val progressPercent: Double
        get() {
            val f1RM = first1RM
            val l1RM = last1RM

            return if (f1RM != null && f1RM > 0 && l1RM != null) {
                ((l1RM - f1RM) / f1RM) * 100
            } else {
                // Fallback to simple kg comparison if 1RM can't be calculated
                val fKg = firstKg
                val lKg = lastKg
                if (fKg != null && fKg > 0 && lKg != null) {
                    ((lKg - fKg) / fKg) * 100
                } else 0.0
            }
        }
}


object RepositoryProvider {
    @Volatile
    private var instance: WorkoutRepository? = null

    fun get(app: Application, db: GymTrackerDatabase): WorkoutRepository {
        return instance ?: synchronized(this) {
            instance ?: WorkoutRepository(app, db).also { instance = it }
        }
    }
}
