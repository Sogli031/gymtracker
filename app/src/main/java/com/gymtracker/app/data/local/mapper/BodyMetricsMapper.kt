package com.gymtracker.app.data.local.mapper

import com.gymtracker.app.data.local.entity.NutritionProfileEntity
import com.gymtracker.app.data.local.entity.WeightEntryEntity
import com.gymtracker.app.data.model.ActivityLevel
import com.gymtracker.app.data.model.Gender
import com.gymtracker.app.data.model.NutritionProfile
import com.gymtracker.app.data.model.WeightEntry

object BodyMetricsMapper {

    fun weightEntryToEntity(entry: WeightEntry): WeightEntryEntity {
        return WeightEntryEntity(
            id = entry.id,
            weightKg = entry.weightKg,
            date = entry.date,
            createdAt = entry.createdAt
        )
    }

    fun weightEntryToDomain(entity: WeightEntryEntity): WeightEntry {
        return WeightEntry(
            id = entity.id,
            weightKg = entity.weightKg,
            date = entity.date,
            createdAt = entity.createdAt
        )
    }

    fun profileToEntity(profile: NutritionProfile): NutritionProfileEntity {
        return NutritionProfileEntity(
            id = 1,
            heightCm = profile.heightCm,
            birthYear = profile.birthYear,
            gender = profile.gender.name,
            activityLevel = profile.activityLevel.name,
            goalWeightKg = profile.goalWeightKg
        )
    }

    fun profileToDomain(entity: NutritionProfileEntity): NutritionProfile {
        return NutritionProfile(
            heightCm = entity.heightCm,
            birthYear = entity.birthYear,
            gender = Gender.valueOf(entity.gender),
            activityLevel = ActivityLevel.valueOf(entity.activityLevel),
            goalWeightKg = entity.goalWeightKg
        )
    }
}
