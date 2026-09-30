package com.gymtracker.app.data.local.mapper

import com.gymtracker.app.data.local.entity.CustomExerciseEntity
import com.gymtracker.app.data.model.CustomExercise
import com.gymtracker.app.data.model.ExerciseCategory

object CustomExerciseMapper {

    fun toEntity(exercise: CustomExercise): CustomExerciseEntity {
        return CustomExerciseEntity(
            id = exercise.id,
            name = exercise.name,
            category = exercise.category.name,
            isCompound = exercise.isCompound
        )
    }

    fun toDomain(entity: CustomExerciseEntity): CustomExercise {
        return CustomExercise(
            id = entity.id,
            name = entity.name,
            // Bezbedno parsiranje: nepoznata kategorija ne sme da obori collector.
            category = ExerciseCategory.fromName(entity.category) ?: ExerciseCategory.KARDIO,
            isCompound = entity.isCompound
        )
    }
}
