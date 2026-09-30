package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_exercises",
    indices = [
        Index("category", name = "idx_ce_category"),
        Index("name", name = "idx_ce_name")
    ]
)
data class CustomExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    @ColumnInfo(name = "is_compound") val isCompound: Boolean
)
