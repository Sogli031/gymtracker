package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "steroid_cycles",
    indices = [Index(value = ["start_date", "end_date"], name = "idx_sc_dates")]
)
data class SteroidCycleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String,
    val name: String?
)
