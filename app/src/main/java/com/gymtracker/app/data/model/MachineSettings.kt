package com.gymtracker.app.data.model

/**
 * Jedno podešavanje mašine (npr. "Visina sedišta" = "5").
 */
data class MachineSetting(
    val name: String,
    val value: String
)

/**
 * Sva podešavanja mašine za jednu vežbu.
 * exerciseName je ključ — vezuje se za ime vežbe (case-insensitive matching).
 */
data class MachineSettings(
    val exerciseName: String,
    val settings: List<MachineSetting> = emptyList()
)
