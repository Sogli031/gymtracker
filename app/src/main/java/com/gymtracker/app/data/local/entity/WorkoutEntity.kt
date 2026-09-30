package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    indices = [Index(value = ["date"], name = "idx_workouts_date")]
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val date: String,       // yyyy-MM-dd
    val time: String,       // HH:mm:ss
    @ColumnInfo(name = "end_time") val endTime: String?,
    @ColumnInfo(name = "created_at") val createdAt: String  // ISO LocalDateTime
)
