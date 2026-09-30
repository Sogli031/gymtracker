package com.gymtracker.app.data.model

import java.util.UUID

data class CustomExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: ExerciseCategory,
    val isCompound: Boolean = false
) {
    fun toExercise(): Exercise = Exercise(
        name = name,
        category = category,
        isCompound = isCompound
    )

    companion object {
        /** Regex za validno ime vežbe - slova, srpski karakteri, brojevi, razmaci, interpunkcija */
        private val NAME_REGEX = Regex("^[a-zA-ZšđčćžŠĐČĆŽ0-9\\s\\-'.,()]+$")

        const val MAX_NAME_LENGTH = 100

        fun isNameValid(name: String): Boolean {
            return name.isNotBlank()
                && name.length <= MAX_NAME_LENGTH
                && NAME_REGEX.matches(name)
        }

        fun validateName(name: String): String? {
            return when {
                name.isBlank() -> "Ime vežbe ne može biti prazno"
                name.length > MAX_NAME_LENGTH -> "Ime vežbe ne može biti duže od $MAX_NAME_LENGTH karaktera"
                !NAME_REGEX.matches(name) -> "Ime vežbe sadrži nedozvoljene karaktere"
                else -> null  // nema greške
            }
        }
    }
}
