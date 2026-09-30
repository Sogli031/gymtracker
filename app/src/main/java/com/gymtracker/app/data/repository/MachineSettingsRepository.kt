package com.gymtracker.app.data.repository

import android.app.Application
import android.util.Log
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.mapper.MachineSettingsMapper
import com.gymtracker.app.data.model.MachineSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
private const val TAG = "MachineSettingsRepo"

class MachineSettingsRepository(private val app: Application, private val db: GymTrackerDatabase) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val machineSettingsDao = db.machineSettingsDao()

    private val _settings = MutableStateFlow<List<MachineSettings>>(emptyList())
    val settings: StateFlow<List<MachineSettings>> = _settings

    init {
        ioScope.launch {
            try {
                launch { collectFromRoom() }
            } catch (e: Exception) {
                Log.e(TAG, "Inicijalizacija neuspešna", e)
            }
        }
    }

    private suspend fun collectFromRoom() {
        machineSettingsDao.observeAll().collect { entities ->
            _settings.value = entities.map { MachineSettingsMapper.toDomain(it) }
        }
    }

    /**
     * Vraća podešavanja mašine za datu vežbu (case-insensitive).
     */
    fun getForExercise(exerciseName: String): MachineSettings? {
        return _settings.value.firstOrNull {
            it.exerciseName.equals(exerciseName, ignoreCase = true)
        }
    }

    /**
     * Čuva ili ažurira podešavanja mašine za vežbu.
     * Ako su settings prazni, briše unos.
     */
    fun save(machineSettings: MachineSettings) {
        ioScope.launch {
            if (machineSettings.settings.isEmpty()) {
                machineSettingsDao.deleteByExerciseName(machineSettings.exerciseName)
            } else {
                // Case-insensitive upsert: jedinstveni indeks na exercise_name je
                // case-sensitive, a svi lookup-i koriste COLLATE NOCASE. Zato prvo
                // uklonimo postojeći red sa istim imenom (NOCASE) da ne nastanu
                // duplikati poput "Bench" i "bench".
                machineSettingsDao.deleteByExerciseName(machineSettings.exerciseName)
                machineSettingsDao.insert(MachineSettingsMapper.toEntity(machineSettings))
            }
        }
    }
}

object MachineSettingsRepositoryProvider {
    @Volatile
    private var instance: MachineSettingsRepository? = null

    fun get(app: Application, db: GymTrackerDatabase): MachineSettingsRepository {
        return instance ?: synchronized(this) {
            instance ?: MachineSettingsRepository(app, db).also { instance = it }
        }
    }
}
