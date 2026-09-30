package com.gymtracker.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymtracker.app.data.local.dao.BodyMetricsDao
import com.gymtracker.app.data.local.dao.CustomExerciseDao
import com.gymtracker.app.data.local.dao.CycleDao
import com.gymtracker.app.data.local.dao.MachineSettingsDao
import com.gymtracker.app.data.local.dao.WorkoutDao
import com.gymtracker.app.data.local.entity.CustomExerciseEntity
import com.gymtracker.app.data.local.entity.ExerciseSetEntity
import com.gymtracker.app.data.local.entity.MachineSettingsEntity
import com.gymtracker.app.data.local.entity.NutritionProfileEntity
import com.gymtracker.app.data.local.entity.SteroidCycleEntity
import com.gymtracker.app.data.local.entity.WeightEntryEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class,
        WeightEntryEntity::class,
        NutritionProfileEntity::class,
        CustomExerciseEntity::class,
        SteroidCycleEntity::class,
        MachineSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GymTrackerDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyMetricsDao(): BodyMetricsDao
    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun cycleDao(): CycleDao
    abstract fun machineSettingsDao(): MachineSettingsDao
}
