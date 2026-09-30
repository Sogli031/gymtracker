package com.gymtracker.app.data.backup

import androidx.room.withTransaction
import com.gymtracker.app.data.local.GymTrackerDatabase
import com.gymtracker.app.data.local.mapper.BodyMetricsMapper
import com.gymtracker.app.data.local.mapper.CustomExerciseMapper
import com.gymtracker.app.data.local.mapper.CycleMapper
import com.gymtracker.app.data.local.mapper.MachineSettingsMapper
import com.gymtracker.app.data.local.mapper.WorkoutMapper
import com.gymtracker.app.data.model.BodyMetricsState
import com.gymtracker.app.data.model.NutritionProfile

/**
 * Veza backup dokumenta i Room baze.
 *
 * Izvoz čita DAO-e (ne in-memory StateFlow-ove, koji mogu biti prazni ili zastareli)
 * u JEDNOJ transakciji, pa je snimak konzistentan. Vraćanje prvo mapira sve u entitete,
 * a tek onda u JEDNOJ transakciji menja tabele — prekid usred posla ostavlja staro stanje.
 */
class BackupStore(private val db: GymTrackerDatabase) {

    suspend fun export(): BackupDocument = db.withTransaction {
        val workoutDao = db.workoutDao()
        val bodyDao = db.bodyMetricsDao()

        val workouts = WorkoutMapper.toDomainList(
            workoutDao.getAll(),
            workoutDao.getAllExercises(),
            workoutDao.getAllSets()
        )
        val profile = bodyDao.getNutritionProfile()?.let { BodyMetricsMapper.profileToDomain(it) }
            ?: NutritionProfile()
        val weights = bodyDao.getAllWeightEntries().map { BodyMetricsMapper.weightEntryToDomain(it) }

        BackupDocument(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            workouts = workouts,
            bodyMetrics = BodyMetricsState(profile = profile, weightEntries = weights),
            cycles = db.cycleDao().getAll().map { CycleMapper.toDomain(it) },
            customExercises = db.customExerciseDao().getAll().map { CustomExerciseMapper.toDomain(it) },
            machineSettings = db.machineSettingsDao().getAll().map { MachineSettingsMapper.toDomain(it) }
        )
    }

    /** Broj lokalnih zapisa — za pravilo "prazno ne gazi popunjeno". */
    suspend fun localRecordCount(): Int = export().recordCount

    /**
     * Zamenjuje lokalne podatke sadržajem dokumenta. Sekcije koje su `null` se ne diraju.
     * Dokument mora biti već validiran ([BackupCodec.validate]).
     */
    suspend fun replaceAll(document: BackupDocument) {
        // Mapiranje PRE transakcije: ako bilo šta pukne, baza nije ni taknuta.
        val workoutEntities = document.workouts?.map { WorkoutMapper.toEntities(it) }
        val weightEntities = document.bodyMetrics?.weightEntries?.map { BodyMetricsMapper.weightEntryToEntity(it) }
        val profileEntity = document.bodyMetrics?.profile?.let { BodyMetricsMapper.profileToEntity(it) }
        val cycleEntities = document.cycles?.map { CycleMapper.toEntity(it) }
        val customExerciseEntities = document.customExercises?.map { CustomExerciseMapper.toEntity(it) }
        val machineSettingsEntities = document.machineSettings?.map { MachineSettingsMapper.toEntity(it) }

        db.withTransaction {
            if (workoutEntities != null) {
                val dao = db.workoutDao()
                dao.deleteAll()
                for ((workout, exercises, sets) in workoutEntities) {
                    dao.insertFullWorkout(workout, exercises, sets)
                }
            }
            if (weightEntities != null && profileEntity != null) {
                val dao = db.bodyMetricsDao()
                dao.deleteAllWeightEntries()
                dao.deleteNutritionProfile()
                weightEntities.forEach { dao.insertWeightEntry(it) }
                dao.insertNutritionProfile(profileEntity)
            }
            if (cycleEntities != null) {
                db.cycleDao().deleteAll()
                if (cycleEntities.isNotEmpty()) db.cycleDao().insertAll(cycleEntities)
            }
            if (customExerciseEntities != null) {
                db.customExerciseDao().deleteAll()
                if (customExerciseEntities.isNotEmpty()) db.customExerciseDao().insertAll(customExerciseEntities)
            }
            if (machineSettingsEntities != null) {
                db.machineSettingsDao().deleteAll()
                if (machineSettingsEntities.isNotEmpty()) db.machineSettingsDao().insertAll(machineSettingsEntities)
            }
        }
    }
}
