package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtracker.app.data.local.entity.SteroidCycleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {

    @Query("SELECT * FROM steroid_cycles ORDER BY start_date DESC")
    fun observeAll(): Flow<List<SteroidCycleEntity>>

    @Query("SELECT * FROM steroid_cycles ORDER BY start_date DESC")
    suspend fun getAll(): List<SteroidCycleEntity>

    @Query("SELECT * FROM steroid_cycles WHERE :date BETWEEN start_date AND end_date LIMIT 1")
    suspend fun getCycleForDate(date: String): SteroidCycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cycle: SteroidCycleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cycles: List<SteroidCycleEntity>)

    @Query("DELETE FROM steroid_cycles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM steroid_cycles")
    suspend fun deleteAll()
}
