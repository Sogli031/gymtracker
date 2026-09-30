package com.gymtracker.app.data.repository

import android.app.Application
import android.util.Log
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.mapper.CycleMapper
import com.gymtracker.app.data.model.SteroidCycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TAG = "CycleRepository"

class CycleRepository(private val app: Application, private val db: GymTrackerDatabase) {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cycleDao = db.cycleDao()

    private val _cycles = MutableStateFlow<List<SteroidCycle>>(emptyList())
    val cycles: StateFlow<List<SteroidCycle>> = _cycles

    init {
        ioScope.launch {
            collectFromRoom()
        }
    }

    private suspend fun collectFromRoom() {
        cycleDao.observeAll().collect { entities ->
            _cycles.value = entities.map { CycleMapper.toDomain(it) }
        }
    }

    fun add(cycle: SteroidCycle) {
        ioScope.launch {
            cycleDao.insert(CycleMapper.toEntity(cycle))
        }
    }

    fun delete(id: String) {
        ioScope.launch {
            cycleDao.delete(id)
        }
    }

    fun update(cycle: SteroidCycle) {
        ioScope.launch {
            cycleDao.insert(CycleMapper.toEntity(cycle))
        }
    }

    /**
     * Proverava da li je dati datum unutar nekog ciklusa.
     */
    fun isOnCycle(date: LocalDate): Boolean {
        return _cycles.value.any { it.contains(date) }
    }

    /**
     * Vraća ciklus koji sadrži dati datum, ili null.
     */
    fun getCycleForDate(date: LocalDate): SteroidCycle? {
        return _cycles.value.firstOrNull { it.contains(date) }
    }
}

object CycleRepositoryProvider {
    @Volatile
    private var instance: CycleRepository? = null

    fun get(app: Application, db: GymTrackerDatabase): CycleRepository {
        return instance ?: synchronized(this) {
            instance ?: CycleRepository(app, db).also { instance = it }
        }
    }
}
