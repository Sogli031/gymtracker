package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exercise_id", name = "idx_es_exercise_id")]
)
data class ExerciseSetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val kg: Double?,
    val reps: Int?,
    @ColumnInfo(name = "order_index") val orderIndex: Int
)
