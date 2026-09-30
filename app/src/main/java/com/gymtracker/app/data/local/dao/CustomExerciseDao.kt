package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtracker.app.data.local.entity.CustomExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomExerciseDao {

    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CustomExerciseEntity>>

    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CustomExerciseEntity>

    @Query("SELECT * FROM custom_exercises WHERE id = :id")
    suspend fun getById(id: String): CustomExerciseEntity?

    @Query("SELECT * FROM custom_exercises WHERE category = :category ORDER BY name COLLATE NOCASE")
    suspend fun getByCategory(category: String): List<CustomExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: CustomExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<CustomExerciseEntity>)

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM custom_exercises")
    suspend fun deleteAll()
}
