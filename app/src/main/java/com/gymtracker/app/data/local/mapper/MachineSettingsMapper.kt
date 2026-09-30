package com.gymtracker.app.data.local.mapper

import com.google.gson.reflect.TypeToken
import com.gymtracker.app.data.local.entity.MachineSettingsEntity
import com.gymtracker.app.data.model.MachineSetting
import com.gymtracker.app.data.model.MachineSettings
import com.gymtracker.app.util.GsonProvider

object MachineSettingsMapper {

    private val gson = GsonProvider.instance

    fun toEntity(settings: MachineSettings): MachineSettingsEntity {
        return MachineSettingsEntity(
            exerciseName = settings.exerciseName,
            settingsJson = gson.toJson(settings.settings)
        )
    }

    fun toDomain(entity: MachineSettingsEntity): MachineSettings {
        val type = object : TypeToken<List<MachineSetting>>() {}.type
        val settingsList: List<MachineSetting> = gson.fromJson(entity.settingsJson, type) ?: emptyList()
        return MachineSettings(
            exerciseName = entity.exerciseName,
            settings = settingsList
        )
    }
}
