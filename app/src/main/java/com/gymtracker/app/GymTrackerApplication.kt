package com.gymtracker.app

import android.app.Application
import android.util.Log
import com.gymtracker.app.data.backup.BackupScheduler
import com.gymtracker.app.data.local.DatabaseProvider
import com.gymtracker.app.data.local.JsonToRoomMigrator
import com.programi.crashlog.CrashLogStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymTrackerApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        CrashLogStorage.install(this, getString(R.string.app_name))

        // JSON->Room migracija se izvršava na pozadinskoj niti (ne blokira UI/main thread,
        // pa nema ANR-a ni kod velikih JSON fajlova). Migracija je jednosmerna i idempotentna;
        // repozitorijumi reaktivno primaju podatke preko Room Flow-a čim se ubace.
        val db = DatabaseProvider.get(this)
        appScope.launch {
            try {
                JsonToRoomMigrator.migrate(this@GymTrackerApplication, db)
            } catch (e: Exception) {
                Log.e("GymTrackerApp", "Greška pri migraciji", e)
            }
            // Tek posle migracije: inače bi samo prebacivanje JSON->Room okinulo backup.
            BackupScheduler.observeDatabase(this@GymTrackerApplication, db)
        }

        // Dnevni backup ako korisnik dugo ne menja ništa (KEEP: ne resetuje se na svakom startu)
        BackupScheduler.schedulePeriodic(this)

        // Repozitorijumi se inicijalizuju u MainActivity.onCreate()
        // posle provere crash loga, da bi crash log mogao da se prikaže
    }
}
