package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtracker.app.data.local.entity.MachineSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineSettingsDao {

    @Query("SELECT * FROM machine_settings ORDER BY exercise_name COLLATE NOCASE")
    fun observeAll(): Flow<List<MachineSettingsEntity>>

    @Query("SELECT * FROM machine_settings ORDER BY exercise_name COLLATE NOCASE")
    suspend fun getAll(): List<MachineSettingsEntity>

    @Query("SELECT * FROM machine_settings WHERE exercise_name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByExerciseName(name: String): MachineSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: MachineSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<MachineSettingsEntity>)

    @Query("DELETE FROM machine_settings WHERE exercise_name = :name COLLATE NOCASE")
    suspend fun deleteByExerciseName(name: String)

    @Query("DELETE FROM machine_settings")
    suspend fun deleteAll()
}
