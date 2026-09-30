package com.gymtracker.app.data.local.mapper

import com.gymtracker.app.data.local.entity.SteroidCycleEntity
import com.gymtracker.app.data.model.Config
import com.gymtracker.app.data.model.SteroidCycle
import java.time.LocalDate

object CycleMapper {

    fun toEntity(cycle: SteroidCycle): SteroidCycleEntity {
        return SteroidCycleEntity(
            id = cycle.id,
            startDate = cycle.startDate.format(Config.storageFormatter),
            endDate = cycle.endDate.format(Config.storageFormatter),
            name = cycle.name
        )
    }

    fun toDomain(entity: SteroidCycleEntity): SteroidCycle {
        return SteroidCycle(
            id = entity.id,
            startDate = LocalDate.parse(entity.startDate, Config.storageFormatter),
            endDate = LocalDate.parse(entity.endDate, Config.storageFormatter),
            name = entity.name
        )
    }
}
