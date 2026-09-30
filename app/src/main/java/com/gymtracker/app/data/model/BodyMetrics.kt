package com.gymtracker.app.data.model

import java.time.LocalDate
import java.util.UUID

/**
 * A single weight entry recorded by the user.
 */
data class WeightEntry(
    val id: String = UUID.randomUUID().toString(),
    val weightKg: Float,
    val date: String, // ISO format: "2026-01-21"
    val createdAt: Long = System.currentTimeMillis()
) {
    val dateAsLocalDate: LocalDate?
        get() = try {
            LocalDate.parse(date, Config.storageFormatter)
        } catch (e: Exception) {
            null
        }
}

/**
 * User's body profile for calorie calculations.
 */
data class NutritionProfile(
    val heightCm: Int = 175,
    val birthYear: Int = 1995,
    val gender: Gender = Gender.MALE,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalWeightKg: Float? = null
)

enum class Gender(val displayName: String) {
    MALE("Muški"),
    FEMALE("Ženski")
}

enum class ActivityLevel(val displayName: String, val multiplier: Float) {
    SEDENTARY("Sedeći (malo kretanja)", 1.2f),
    LIGHT("Lagano aktivan (1-2x trening)", 1.375f),
    MODERATE("Umereno aktivan (3-4x trening)", 1.55f),
    ACTIVE("Aktivan (5-6x trening)", 1.725f),
    VERY_ACTIVE("Veoma aktivan (fizički posao)", 1.9f)
}

/**
 * Complete body metrics state with profile and weight history.
 */
data class BodyMetricsState(
    val profile: NutritionProfile = NutritionProfile(),
    val weightEntries: List<WeightEntry> = emptyList()
) {
    /**
     * Weight entries sorted chronologically by the actual measurement date
     * (oldest first), with insertion time as tie-breaker. Entries with an
     * unparseable date sink to the bottom (treated as oldest).
     */
    private val entriesByDate: List<WeightEntry>
        get() = weightEntries.sortedWith(
            compareBy({ it.dateAsLocalDate ?: LocalDate.MIN }, { it.createdAt })
        )

    /**
     * Get the most recent weight entry (by measurement date, not insertion time).
     */
    val currentWeight: Float?
        get() = entriesByDate.lastOrNull()?.weightKg

    /**
     * Get weight entries sorted by measurement date (oldest first).
     */
    val weightHistory: List<WeightEntry>
        get() = entriesByDate

    /**
     * Calculate age from birth year.
     */
    val age: Int
        get() = LocalDate.now().year - profile.birthYear

    /**
     * BMR (Harris-Benedict) as a full-precision value; single source of truth
     * so that [bmr] and [tdee] stay consistent (no premature Int truncation).
     * BMR (men): 88.362 + (13.397 × kg) + (4.799 × cm) - (5.677 × years)
     * BMR (women): 447.593 + (9.247 × kg) + (3.098 × cm) - (4.330 × years)
     */
    private val bmrRaw: Double?
        get() {
            val weight = currentWeight ?: return null
            return when (profile.gender) {
                Gender.MALE -> 88.362 + (13.397 * weight) + (4.799 * profile.heightCm) - (5.677 * age)
                Gender.FEMALE -> 447.593 + (9.247 * weight) + (3.098 * profile.heightCm) - (4.330 * age)
            }
        }

    /**
     * Calculate BMR using Harris-Benedict formula.
     */
    val bmr: Int?
        get() = bmrRaw?.toInt()

    /**
     * Calculate TDEE (Total Daily Energy Expenditure).
     * TDEE = BMR × activity level multiplier (computed from full-precision BMR).
     */
    val tdee: Int?
        get() = bmrRaw?.let { (it * profile.activityLevel.multiplier).toInt() }

    /**
     * Calculate target calories for muscle growth.
     * Target = TDEE + 400 (moderate surplus for lean gains)
     */
    val targetCaloriesForGrowth: Int?
        get() = tdee?.plus(Config.CALORIE_SURPLUS)

    /**
     * Calculate protein target in grams (2g per kg body weight).
     */
    val proteinTarget: Int?
        get() = currentWeight?.let { (it * Config.PROTEIN_PER_KG).toInt() }

    /**
     * Weight change from oldest to newest entry.
     */
    val totalWeightChange: Float?
        get() {
            if (weightEntries.size < 2) return null
            val sorted = entriesByDate
            val oldest = sorted.firstOrNull()?.weightKg ?: return null
            val newest = sorted.lastOrNull()?.weightKg ?: return null
            return newest - oldest
        }

    /**
     * Progress towards goal weight (0.0 to 1.0+)
     */
    val goalProgress: Float?
        get() {
            val goal = profile.goalWeightKg ?: return null
            val current = currentWeight ?: return null
            val start = entriesByDate.firstOrNull()?.weightKg ?: return null
            if (goal == start) return 1f
            return (current - start) / (goal - start)
        }

    /**
     * Check if in caloric surplus for growth.
     * This is a conceptual check - actual tracking would need food logging.
     */
    fun isGainingWeight(): Boolean? {
        if (weightEntries.size < 2) return null
        val recentEntries = entriesByDate.takeLast(3)
        if (recentEntries.size < 2) return null
        val newest = recentEntries.last().weightKg
        val older = recentEntries.first().weightKg
        return newest > older
    }
}

/**
 * Helper for calorie and macro calculations.
 */
object CalorieCalculator {

    /**
     * Calculate maintenance calories given profile and weight.
     */
    fun calculateTDEE(
        weightKg: Float,
        heightCm: Int,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel
    ): Int {
        val bmr = when (gender) {
            Gender.MALE -> 88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age)
            Gender.FEMALE -> 447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age)
        }
        return (bmr * activityLevel.multiplier).toInt()
    }

    /**
     * Calculate macros based on a 60/25/15 ratio.
     * Returns Triple of (carbs, protein, fat) in grams.
     */
    fun calculateMacros(totalCalories: Int): Triple<Int, Int, Int> {
        val carbCalories = (totalCalories * Config.MACRO_CARBS_RATIO).toInt()
        val proteinCalories = (totalCalories * Config.MACRO_PROTEIN_RATIO).toInt()
        val fatCalories = (totalCalories * Config.MACRO_FAT_RATIO).toInt()

        val carbs = carbCalories / Config.CALORIES_PER_GRAM_CARB
        val protein = proteinCalories / Config.CALORIES_PER_GRAM_PROTEIN
        val fat = fatCalories / Config.CALORIES_PER_GRAM_FAT

        return Triple(carbs, protein, fat)
    }
}
