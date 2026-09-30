package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_entries",
    indices = [Index("created_at", name = "idx_we_created_at")]
)
data class WeightEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "weight_kg") val weightKg: Float,
    val date: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
