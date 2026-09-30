package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtracker.app.data.local.entity.NutritionProfileEntity
import com.gymtracker.app.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricsDao {

    // === Weight entries ===

    @Query("SELECT * FROM weight_entries ORDER BY created_at DESC")
    fun observeWeightEntries(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY created_at DESC")
    suspend fun getAllWeightEntries(): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteWeightEntry(id: String)

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAllWeightEntries()

    // === Nutrition profile (singlton, id=1) ===

    @Query("SELECT * FROM nutrition_profile WHERE id = 1")
    fun observeNutritionProfile(): Flow<NutritionProfileEntity?>

    @Query("SELECT * FROM nutrition_profile WHERE id = 1")
    suspend fun getNutritionProfile(): NutritionProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionProfile(profile: NutritionProfileEntity)

    @Query("DELETE FROM nutrition_profile")
    suspend fun deleteNutritionProfile()
}
