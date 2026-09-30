package com.gymtracker.app.data.backup

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gymtracker.app.data.local.DatabaseProvider

/**
 * Pozadinski posao koji šalje backup na Google Drive. Pokreće ga [BackupScheduler]
 * (posle izmene + dnevno), uz uslov da ima mreže.
 *
 * Primenjuje ista pravila kao [BackupCoordinator.backup], samo bez pitanja korisniku:
 * prazno stanje se ne šalje, a dok oblak nije „usvojen" na ovoj instalaciji ništa se
 * ne prepisuje — tu odluku korisnik donosi na ekranu.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? Application ?: return Result.success()
        val drive = BackupProvider.get(app)

        if (!drive.isSignedIn()) {
            Log.d(TAG, "Preskačem auto-backup: korisnik nije prijavljen")
            return Result.success()
        }

        return try {
            val store = BackupStore(DatabaseProvider.get(app))
            val document = store.export()

            // "Prazno ne gazi popunjeno": prazan dokument u oblaku bi samo zaklonio
            // poslednji dobar backup.
            if (document.isEmpty) {
                Log.d(TAG, "Preskačem auto-backup: nema podataka")
                return Result.success()
            }

            // Baca izuzetak ako provera ne uspe - tada se NIŠTA ne otprema.
            if (drive.cloudBackupExists() && !drive.isCloudAdopted()) {
                Log.d(TAG, "Preskačem auto-backup: oblak nije preuzet na ovoj instalaciji")
                return Result.success()
            }

            drive.uploadCombined(BackupCodec.encode(document))
            drive.recordSuccessfulSync()
            Log.d(TAG, "Auto-backup uspešan (${document.summary()})")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Auto-backup neuspešan: ${e.message}")
            if (runAttemptCount >= MAX_RETRY_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val MAX_RETRY_ATTEMPTS = 7
    }
}
