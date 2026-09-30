package com.gymtracker.app.data.model

import java.util.UUID

data class ExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    val kg: Double? = null,
    val reps: Int? = null
) {
    val isComplete: Boolean
        get() = kg != null && reps != null

    /**
     * Estimated 1 Rep Max using Brzycki formula (R² = 0.99):
     * 1RM = kg / (1.0278 - 0.0278 × reps)
     *
     * More accurate than Epley for <10 reps.
     * Used to compare performance across different weight/rep combinations.
     * Example: 80kg×8 ≈ 99.3kg 1RM, 90kg×5 ≈ 101.3kg 1RM
     */
    val estimated1RM: Double?
        get() {
            if (kg == null || reps == null || reps == 0) return null
            if (reps == 1) return kg
            if (reps > Config.BRZYCKI_MAX_REPS) return null
            val denominator = 1.0278 - 0.0278 * reps
            return if (denominator > 0) kg / denominator else null
        }

    /**
     * Display string for estimated 1RM
     */
    val display1RM: String?
        get() = estimated1RM?.let { "${String.format("%.1f", it)}kg 1RM" }

    /** Da li su vrednosti u prihvatljivom opsegu */
    val isValid: Boolean
        get() {
            val k = kg ?: return true  // null je OK (prazan set)
            val r = reps ?: return true
            return k in 0.0..500.0 && r in 0..200
        }

    /** Vraća listu grešaka validacije na srpskom */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        kg?.let { k ->
            if (k < 0) errors.add("Težina ne može biti negativna")
            if (k > 500) errors.add("Težina ne može biti veća od 500 kg")
        }
        reps?.let { r ->
            if (r < 0) errors.add("Broj ponavljanja ne može biti negativan")
            if (r > 200) errors.add("Broj ponavljanja ne može biti veći od 200")
        }
        return errors
    }
}

