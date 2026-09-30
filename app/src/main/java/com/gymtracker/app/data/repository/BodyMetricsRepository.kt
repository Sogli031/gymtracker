package com.gymtracker.app.data.repository

import android.app.Application
import android.util.Log
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.mapper.BodyMetricsMapper
import com.gymtracker.app.data.model.ActivityLevel
import com.gymtracker.app.data.model.BodyMetricsState
import com.gymtracker.app.data.model.Config
import com.gymtracker.app.data.model.Gender
import com.gymtracker.app.data.model.NutritionProfile
import com.gymtracker.app.data.model.WeightEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "BodyMetricsRepository"

class BodyMetricsRepository(private val app: Application, private val db: GymTrackerDatabase) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bodyMetricsDao = db.bodyMetricsDao()

    private val _state = MutableStateFlow(BodyMetricsState())
    val state: StateFlow<BodyMetricsState> = _state

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    init {
        ioScope.launch {
            launch {
                bodyMetricsDao.observeWeightEntries().collect { entities ->
                    val entries = entities.map { BodyMetricsMapper.weightEntryToDomain(it) }
                    _state.update { it.copy(weightEntries = entries) }
                }
            }
            launch {
                bodyMetricsDao.observeNutritionProfile().collect { entity ->
                    val profile = entity?.let { BodyMetricsMapper.profileToDomain(it) } ?: NutritionProfile()
                    _state.update { it.copy(profile = profile) }
                }
            }
            _isLoaded.value = true
        }
    }

    /**
     * Add a new weight entry.
     */
    fun addWeightEntry(weightKg: Float, date: LocalDate = LocalDate.now()) {
        val entry = WeightEntry(
            weightKg = weightKg,
            date = date.format(Config.storageFormatter)
        )
        ioScope.launch {
            bodyMetricsDao.insertWeightEntry(BodyMetricsMapper.weightEntryToEntity(entry))
        }
        Log.d(TAG, "Dodat unos težine: ${weightKg}kg")
    }

    /**
     * Delete a weight entry by ID.
     */
    fun deleteWeightEntry(id: String) {
        ioScope.launch {
            bodyMetricsDao.deleteWeightEntry(id)
        }
        Log.d(TAG, "Obrisan unos težine: $id")
    }

    /**
     * Update the nutrition profile.
     */
    fun updateProfile(
        heightCm: Int? = null,
        birthYear: Int? = null,
        gender: Gender? = null,
        activityLevel: ActivityLevel? = null,
        goalWeightKg: Float? = null
    ) {
        _state.update { current ->
            val p = current.profile
            current.copy(
                profile = p.copy(
                    heightCm = heightCm ?: p.heightCm,
                    birthYear = birthYear ?: p.birthYear,
                    gender = gender ?: p.gender,
                    activityLevel = activityLevel ?: p.activityLevel,
                    goalWeightKg = goalWeightKg ?: p.goalWeightKg
                )
            )
        }
        ioScope.launch {
            bodyMetricsDao.insertNutritionProfile(BodyMetricsMapper.profileToEntity(_state.value.profile))
        }
        Log.d(TAG, "Ažuriran profil ishrane")
    }

    /**
     * Set full profile at once.
     */
    fun setProfile(profile: NutritionProfile) {
        _state.update { it.copy(profile = profile) }
        ioScope.launch {
            bodyMetricsDao.insertNutritionProfile(BodyMetricsMapper.profileToEntity(profile))
        }
        Log.d(TAG, "Postavljen profil ishrane")
    }

    /**
     * Clear goal weight.
     */
    fun clearGoalWeight() {
        _state.update { it.copy(profile = it.profile.copy(goalWeightKg = null)) }
        ioScope.launch {
            bodyMetricsDao.insertNutritionProfile(BodyMetricsMapper.profileToEntity(_state.value.profile))
        }
        Log.d(TAG, "Obrisana ciljna težina")
    }

}

object BodyMetricsRepositoryProvider {
    @Volatile
    private var instance: BodyMetricsRepository? = null

    fun get(app: Application, db: GymTrackerDatabase): BodyMetricsRepository {
        return instance ?: synchronized(this) {
            instance ?: BodyMetricsRepository(app, db).also { instance = it }
        }
    }
}
