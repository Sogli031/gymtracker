package com.gymtracker.app.data.backup

import com.gymtracker.app.data.model.ActivityLevel
import com.gymtracker.app.data.model.BodyMetricsState
import com.gymtracker.app.data.model.CustomExercise
import com.gymtracker.app.data.model.ExerciseCategory
import com.gymtracker.app.data.model.ExerciseSet
import com.gymtracker.app.data.model.Gender
import com.gymtracker.app.data.model.MachineSetting
import com.gymtracker.app.data.model.MachineSettings
import com.gymtracker.app.data.model.NutritionProfile
import com.gymtracker.app.data.model.SteroidCycle
import com.gymtracker.app.data.model.WeightEntry
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.data.model.WorkoutExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BackupCodecTest {

    private fun populatedDocument() = BackupDocument(
        schemaVersion = BACKUP_SCHEMA_VERSION,
        exportedAt = 1_700_000_000_000L,
        workouts = listOf(
            Workout(
                id = "w1",
                name = "Push dan",
                date = LocalDate.of(2026, 3, 14),
                time = LocalTime.of(18, 5, 30),
                endTime = LocalTime.of(19, 1),
                exercises = listOf(
                    WorkoutExercise(
                        id = "e1",
                        exerciseName = "Bench Press",
                        category = ExerciseCategory.GRUDI,
                        sets = listOf(
                            ExerciseSet(id = "s1", kg = 82.5, reps = 8),
                            ExerciseSet(id = "s2", kg = null, reps = null)
                        )
                    ),
                    WorkoutExercise(
                        id = "e2",
                        exerciseName = "Leđa - moja vežba",
                        category = ExerciseCategory.LEDJA,
                        sets = listOf(ExerciseSet(id = "s3", kg = 40.0, reps = 12))
                    )
                ),
                createdAt = LocalDateTime.of(2026, 3, 14, 18, 5, 31, 123_000_000)
            ),
            Workout(
                id = "w2",
                name = null,
                date = LocalDate.of(2026, 3, 16),
                time = LocalTime.of(7, 0),
                endTime = null,
                exercises = emptyList(),
                createdAt = LocalDateTime.of(2026, 3, 16, 7, 0)
            )
        ),
        bodyMetrics = BodyMetricsState(
            profile = NutritionProfile(
                heightCm = 183,
                birthYear = 1990,
                gender = Gender.FEMALE,
                activityLevel = ActivityLevel.VERY_ACTIVE,
                goalWeightKg = 78.5f
            ),
            weightEntries = listOf(
                WeightEntry(id = "k1", weightKg = 81.3f, date = "2026-03-01", createdAt = 1_700_000_000_001L),
                WeightEntry(id = "k2", weightKg = 80.9f, date = "2026-03-08", createdAt = 1_700_000_000_002L)
            )
        ),
        cycles = listOf(
            SteroidCycle(id = "c1", startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 3, 1), name = "Zimski"),
            SteroidCycle(id = "c2", startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 2, 1), name = null)
        ),
        customExercises = listOf(
            CustomExercise(id = "x1", name = "Cable Fly", category = ExerciseCategory.GRUDI, isCompound = false)
        ),
        machineSettings = listOf(
            MachineSettings("Leg Press", listOf(MachineSetting("Visina sedišta", "5"), MachineSetting("Ugao", "45")))
        )
    )

    @Test
    fun `round trip keeps every field`() {
        val original = populatedDocument()

        val json = BackupCodec.encode(original)
        val decoded = BackupCodec.decode(json)

        assertEquals(original, decoded)
        assertEquals(json, BackupCodec.encode(decoded))
        assertEquals(8, decoded.recordCount)
    }

    @Test
    fun `old local export with workouts only still imports and leaves other sections untouched`() {
        val legacy = """
            [{"id":"w1","name":"Noge","date":"2025-11-02","time":"17:30:00","endTime":"18:20:00",
              "exercises":[{"id":"e1","exerciseName":"Barbell Squat","category":"NOGE",
                            "sets":[{"id":"s1","kg":100.0,"reps":5}]}],
              "createdAt":"2025-11-02T17:30:05"}]
        """.trimIndent()

        val decoded = BackupCodec.decode(legacy)

        assertEquals(1, decoded.workouts?.size)
        assertEquals(100.0, decoded.workouts!![0].exercises[0].sets[0].kg!!, 0.0)
        assertNull(decoded.bodyMetrics)
        assertNull(decoded.cycles)
        assertNull(decoded.customExercises)
        assertNull(decoded.machineSettings)
    }

    @Test
    fun `old five separate cloud files still restore`() {
        val decoded = BackupCodec.decodeLegacy(
            workoutsJson = """[{"id":"w1","date":"2025-11-02","time":"17:30:00","exercises":[],"createdAt":"2025-11-02T17:30:05"}]""",
            bodyMetricsJson = """{"profile":{"heightCm":180,"birthYear":1992,"gender":"MALE","activityLevel":"MODERATE"},
                "weightEntries":[{"id":"k1","weightKg":84.2,"date":"2025-11-01","createdAt":1}]}""",
            cyclesJson = """[{"id":"c1","startDate":"2025-01-01","endDate":"2025-03-01","name":"Test"}]""",
            customExercisesJson = "[]",
            machineSettingsJson = null
        )

        assertEquals(1, decoded.workouts?.size)
        assertEquals(84.2f, decoded.bodyMetrics!!.weightEntries[0].weightKg, 0.0f)
        assertNull(decoded.bodyMetrics!!.profile.goalWeightKg)
        assertEquals(LocalDate.of(2025, 3, 1), decoded.cycles!![0].endDate)
        assertEquals(0, decoded.customExercises?.size)
        assertNull(decoded.machineSettings)
        assertEquals(listOf("podešavanja mašina"), decoded.missingSections())
    }

    @Test
    fun `invalid record fails the whole restore instead of being skipped`() {
        val unknownCategory = BackupCodec.encode(populatedDocument()).replace("\"LEDJA\"", "\"NEPOSTOJI\"")
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(unknownCategory) }

        val missingDate = """[{"id":"w1","time":"17:30:00","exercises":[],"createdAt":"2025-11-02T17:30:05"}]"""
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(missingDate) }

        val duplicateIds = BackupCodec.encode(populatedDocument()).replace("\"w2\"", "\"w1\"")
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(duplicateIds) }
    }

    @Test
    fun `combined document with a missing section or from a newer app is rejected`() {
        val missingSection = """{"schemaVersion":1,"exportedAt":1,"workouts":[]}"""
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(missingSection) }

        val newer = BackupCodec.encode(populatedDocument().copy(schemaVersion = BACKUP_SCHEMA_VERSION + 1))
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode(newer) }

        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("{\"foo\":1}") }
        assertThrows(BackupFormatException::class.java) { BackupCodec.decode("   ") }
    }

    @Test
    fun `empty document is detected`() {
        val empty = BackupDocument(
            workouts = emptyList(),
            bodyMetrics = BodyMetricsState(),
            cycles = emptyList(),
            customExercises = emptyList(),
            machineSettings = emptyList()
        )
        assertTrue(BackupCodec.decode(BackupCodec.encode(empty)).isEmpty)
    }
}
