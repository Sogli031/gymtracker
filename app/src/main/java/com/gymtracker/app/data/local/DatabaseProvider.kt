package com.gymtracker.app.data.local

import android.app.Application
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: GymTrackerDatabase? = null

    fun get(app: Application): GymTrackerDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                app,
                GymTrackerDatabase::class.java,
                "gymtracker.db"
            ).build().also { instance = it }
        }
    }
}
