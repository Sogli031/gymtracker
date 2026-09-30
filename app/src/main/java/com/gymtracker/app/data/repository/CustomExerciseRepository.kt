package com.gymtracker.app.data.repository

import android.app.Application
import android.util.Log
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.mapper.CustomExerciseMapper
import com.gymtracker.app.data.model.CustomExercise
import com.gymtracker.app.data.model.ExerciseCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
private const val TAG = "CustomExerciseRepo"

class CustomExerciseRepository(private val app: Application, private val db: GymTrackerDatabase) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val customExerciseDao = db.customExerciseDao()

    private val _exercises = MutableStateFlow<List<CustomExercise>>(emptyList())
    val exercises: StateFlow<List<CustomExercise>> = _exercises

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    init {
        ioScope.launch { collectFromRoom() }
    }

    private suspend fun collectFromRoom() {
        customExerciseDao.observeAll().collect { entities ->
            try {
                _exercises.value = entities.map { CustomExerciseMapper.toDomain(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri učitavanju custom vežbi iz baze", e)
            } finally {
                _isLoaded.value = true
            }
        }
    }

    fun add(exercise: CustomExercise) {
        ioScope.launch {
            customExerciseDao.insert(CustomExerciseMapper.toEntity(exercise))
        }
    }

    fun update(exercise: CustomExercise) {
        ioScope.launch {
            customExerciseDao.insert(CustomExerciseMapper.toEntity(exercise))
        }
    }

    fun delete(id: String) {
        ioScope.launch {
            customExerciseDao.delete(id)
        }
    }

    fun get(id: String): CustomExercise? = _exercises.value.firstOrNull { it.id == id }

    fun getByCategory(category: ExerciseCategory): List<CustomExercise> {
        return _exercises.value.filter { it.category == category }
    }
}

object CustomExerciseRepositoryProvider {
    @Volatile
    private var instance: CustomExerciseRepository? = null

    fun get(app: Application, db: GymTrackerDatabase): CustomExerciseRepository {
        return instance ?: synchronized(this) {
            instance ?: CustomExerciseRepository(app, db).also { instance = it }
        }
    }
}
