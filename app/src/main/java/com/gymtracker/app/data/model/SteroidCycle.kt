package com.gymtracker.app.data.model

import java.time.LocalDate
import java.util.UUID

data class SteroidCycle(
    val id: String = UUID.randomUUID().toString(),
    val startDate: LocalDate,
    val endDate: LocalDate,
    val name: String? = null
) {
    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }

    val displayRange: String
        get() = "${startDate.format(Config.displayFormatter)} - ${endDate.format(Config.displayFormatter)}"
}
