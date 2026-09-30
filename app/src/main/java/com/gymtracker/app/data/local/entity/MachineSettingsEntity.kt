package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "machine_settings",
    indices = [Index("exercise_name", name = "idx_ms_exercise_name", unique = true)]
)
data class MachineSettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "settings_json") val settingsJson: String
)
