package com.gymtracker.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("workout_id", name = "idx_we_workout_id"),
        Index("exercise_name", name = "idx_we_exercise_name")
    ]
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    val category: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int
)
