package com.gymtracker.app.data.backup

import android.app.Application
import com.gymtracker.app.data.local.DatabaseProvider
import com.gymtracker.app.data.repository.RepositoryProvider

object BackupProvider {
    @Volatile
    private var instance: GoogleDriveBackup? = null

    @Volatile
    private var coordinatorInstance: BackupCoordinator? = null

    fun get(app: Application): GoogleDriveBackup {
        return instance ?: synchronized(this) {
            instance ?: GoogleDriveBackup(app).also { instance = it }
        }
    }

    /** Koordinator živi koliko i proces — backup/vraćanje ne zavise od otvorenog ekrana. */
    fun coordinator(app: Application): BackupCoordinator {
        return coordinatorInstance ?: synchronized(this) {
            coordinatorInstance ?: run {
                val db = DatabaseProvider.get(app)
                BackupCoordinator(
                    drive = get(app),
                    store = BackupStore(db),
                    // Posle zamene podataka keš analize odmora više ne važi.
                    onDataReplaced = { RepositoryProvider.get(app, db).clearCaches() }
                ).also { coordinatorInstance = it }
            }
        }
    }
}
