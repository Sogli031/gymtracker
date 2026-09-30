package com.gymtracker.app.data.local

import android.app.Application
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gymtracker.app.data.local.mapper.*
import com.gymtracker.app.data.model.*
import com.gymtracker.app.util.GsonProvider
import java.io.File

private const val TAG = "JsonToRoomMigrator"

object JsonToRoomMigrator {

    /**
     * Izvršava jednosmernu JSON->Room migraciju pri prvom pokretanju.
     * Blokirajuće -- poziva se iz Application.onCreate() pre inicijalizacije repozitorijuma.
     * Bezbedna za ponovljeno pozivanje -- preskače ako su JSON fajlovi već migrirani.
     */
    suspend fun migrate(app: Application, db: GymTrackerDatabase) {
        val gson = GsonProvider.instance
        val filesDir = app.filesDir

        val workoutFile = File(filesDir, Config.DATA_FILE)
        val bodyMetricsFile = File(filesDir, Config.BODY_METRICS_FILE)
        val customExercisesFile = File(filesDir, Config.CUSTOM_EXERCISES_FILE)
        val cyclesFile = File(filesDir, Config.CYCLES_FILE)
        val machineSettingsFile = File(filesDir, Config.MACHINE_SETTINGS_FILE)

        // Preskoči ako nijedan JSON fajl ne postoji
        val anyJsonExists = listOf(workoutFile, bodyMetricsFile, customExercisesFile, cyclesFile, machineSettingsFile)
            .any { it.exists() }
        if (!anyJsonExists) {
            Log.d(TAG, "Nema JSON fajlova za migraciju")
            return
        }

        // Preskoči ako baza već ima podatke (migracija je već izvršena)
        val hasWorkouts = db.workoutDao().getAll().isNotEmpty()
        val hasWeightEntries = db.bodyMetricsDao().getAllWeightEntries().isNotEmpty()
        val hasProfile = db.bodyMetricsDao().getNutritionProfile() != null
        val hasCustomExercises = db.customExerciseDao().getAll().isNotEmpty()
        val hasCycles = db.cycleDao().getAll().isNotEmpty()
        val hasMachineSettings = db.machineSettingsDao().getAll().isNotEmpty()

        if (hasWorkouts || hasWeightEntries || hasProfile || hasCustomExercises || hasCycles || hasMachineSettings) {
            Log.d(TAG, "Room baza već ima podatke, preskačem migraciju")
            // Ipak preimenuj JSON ako nisu prethodno preimenovani
            renameJsonFiles(workoutFile, bodyMetricsFile, customExercisesFile, cyclesFile, machineSettingsFile)
            return
        }

        try {
            Log.d(TAG, "Započinjem JSON->Room migraciju...")

            db.withTransaction {
                // 1. Migracija treninga
                if (workoutFile.exists()) {
                    migrateWorkouts(workoutFile, gson, db)
                }

                // 2. Migracija telesnih metrika
                if (bodyMetricsFile.exists()) {
                    migrateBodyMetrics(bodyMetricsFile, gson, db)
                }

                // 3. Migracija custom vežbi
                if (customExercisesFile.exists()) {
                    migrateCustomExercises(customExercisesFile, gson, db)
                }

                // 4. Migracija ciklusa
                if (cyclesFile.exists()) {
                    migrateCycles(cyclesFile, gson, db)
                }

                // 5. Migracija podešavanja mašina
                if (machineSettingsFile.exists()) {
                    migrateMachineSettings(machineSettingsFile, gson, db)
                }
            }

            // Preimenuj JSON fajlove u .migrated
            renameJsonFiles(workoutFile, bodyMetricsFile, customExercisesFile, cyclesFile, machineSettingsFile)

            Log.d(TAG, "JSON->Room migracija uspešno završena")
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri JSON->Room migraciji -- sledeće pokretanje će ponovo pokušati", e)
        }
    }

    private suspend fun migrateWorkouts(file: File, gson: Gson, db: GymTrackerDatabase) {
        val raw = file.readText()
        if (raw.isBlank()) return
        val workouts: List<Workout> = gson.fromJson(raw, Array<Workout>::class.java)?.toList().orEmpty()
        Log.d(TAG, "Migracija ${workouts.size} treninga...")

        for (workout in workouts) {
            val (we, exes, sets) = WorkoutMapper.toEntities(workout)
            db.workoutDao().insertFullWorkout(we, exes, sets)
        }
    }

    private suspend fun migrateBodyMetrics(file: File, gson: Gson, db: GymTrackerDatabase) {
        val raw = file.readText()
        if (raw.isBlank()) return
        val state = gson.fromJson(raw, BodyMetricsState::class.java) ?: return
        Log.d(TAG, "Migracija telesnih metrika: ${state.weightEntries.size} unosa težine...")

        // Profil
        db.bodyMetricsDao().insertNutritionProfile(BodyMetricsMapper.profileToEntity(state.profile))

        // Unosi težine
        for (entry in state.weightEntries) {
            db.bodyMetricsDao().insertWeightEntry(BodyMetricsMapper.weightEntryToEntity(entry))
        }
    }

    private suspend fun migrateCustomExercises(file: File, gson: Gson, db: GymTrackerDatabase) {
        val raw = file.readText()
        if (raw.isBlank()) return
        val exercises: List<CustomExercise> = gson.fromJson(raw, Array<CustomExercise>::class.java)?.toList().orEmpty()
        Log.d(TAG, "Migracija ${exercises.size} custom vežbi...")

        db.customExerciseDao().insertAll(exercises.map { CustomExerciseMapper.toEntity(it) })
    }

    private suspend fun migrateCycles(file: File, gson: Gson, db: GymTrackerDatabase) {
        val raw = file.readText()
        if (raw.isBlank()) return
        val cycles: List<SteroidCycle> = gson.fromJson(raw, Array<SteroidCycle>::class.java)?.toList().orEmpty()
        Log.d(TAG, "Migracija ${cycles.size} ciklusa...")

        db.cycleDao().insertAll(cycles.map { CycleMapper.toEntity(it) })
    }

    private suspend fun migrateMachineSettings(file: File, gson: Gson, db: GymTrackerDatabase) {
        val raw = file.readText()
        if (raw.isBlank()) return
        val type = object : TypeToken<List<MachineSettings>>() {}.type
        val settings: List<MachineSettings> = gson.fromJson<List<MachineSettings>>(raw, type).orEmpty()
        Log.d(TAG, "Migracija ${settings.size} podešavanja mašina...")

        db.machineSettingsDao().insertAll(settings.map { MachineSettingsMapper.toEntity(it) })
    }

    private fun renameJsonFiles(vararg files: File) {
        for (file in files) {
            if (file.exists()) {
                val migrated = File(file.parent, "${file.name}.migrated")
                if (file.renameTo(migrated)) {
                    Log.d(TAG, "Preimenovan ${file.name} -> ${migrated.name}")
                }
            }
        }
    }
}
