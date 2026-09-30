package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_profile")
data class NutritionProfileEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "height_cm") val heightCm: Int,
    @ColumnInfo(name = "birth_year") val birthYear: Int,
    val gender: String,              // Gender.name
    @ColumnInfo(name = "activity_level") val activityLevel: String, // ActivityLevel.name
    @ColumnInfo(name = "goal_weight_kg") val goalWeightKg: Float?
)
