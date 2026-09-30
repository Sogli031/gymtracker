package com.gymtracker.app.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val TAG = "GoogleDriveBackup"

/** Novi format: JEDAN fajl sa svim podacima. */
private const val COMBINED_FILE_NAME = "gymtracker_full_backup.json"
/** Prethodne kopije kombinovanog fajla: gymtracker_full_backup_prev_<epochMillis>.json */
private const val PREV_PREFIX = "gymtracker_full_backup_prev_"
private const val PREV_SUFFIX = ".json"
private const val PREV_COPIES_TO_KEEP = 5

// Stari format (pet odvojenih fajlova) — samo se ČITA, nikad se ne menja niti briše.
private const val LEGACY_WORKOUTS_FILE = "gymtracker_backup.json"
private const val LEGACY_BODY_METRICS_FILE = "gymtracker_body_metrics.json"
private const val LEGACY_CYCLES_FILE = "gymtracker_cycles.json"
private const val LEGACY_CUSTOM_EXERCISES_FILE = "gymtracker_custom_exercises.json"
private const val LEGACY_MACHINE_SETTINGS_FILE = "gymtracker_machine_settings.json"

private const val APP_DATA_FOLDER = "appDataFolder"
private const val PREFS_NAME = "gymtracker_prefs"
private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
private const val KEY_LAST_SYNC_TIME = "last_sync_time"
private const val KEY_CLOUD_ADOPTED = "cloud_adopted"
private const val AUTH_RECOVERY_MESSAGE =
    "Potrebna je ponovna autorizacija za Google Drive. Odjavite se i prijavite ponovo."

/** Greška Drive operacije sa porukom za korisnika. */
class DriveException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Sadržaj starog formata; `null` = taj fajl ne postoji u oblaku. */
data class LegacyCloudFiles(
    val workouts: String?,
    val bodyMetrics: String?,
    val cycles: String?,
    val customExercises: String?,
    val machineSettings: String?
)

/** Jedna kopija backup-a u oblaku (trenutna ili prethodna). */
data class CloudCopy(
    val fileId: String,
    val createdAtMillis: Long,
    val isCurrent: Boolean
)

/**
 * Niski sloj: prijava i rad sa fajlovima u privatnom Drive folderu aplikacije.
 * Redosled i pravila backup-a/vraćanja su u [BackupCoordinator].
 *
 * Pravila ovog sloja:
 *  - traženje glavnog fajla poklapa TAČNO ime i NIKAD ništa ne briše;
 *  - pre prepisivanja glavnog fajla pravi se prethodna kopija; ako kopija ne uspe,
 *    backup se PREKIDA (dobar backup se ne gazi);
 *  - greška (mreža/autorizacija) je uvek izuzetak, nikad "fajl ne postoji".
 */
class GoogleDriveBackup(private val context: Context) {

    private val _signInState = MutableStateFlow<SignInState>(SignInState.SignedOut)
    val signInState: StateFlow<SignInState> = _signInState

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    // @Volatile: menja se iz init/handleSignInResult i signOut (IO), a čita iz
    // backup/restore korutina na drugim nitima — treba vidljivost između niti.
    @Volatile
    private var driveService: Drive? = null

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        // Check if already signed in
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))) {
            _signInState.value = SignInState.SignedIn(account.email ?: "Unknown")
            initDriveService(account)
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            _signInState.value = SignInState.SignedIn(account.email ?: "Unknown")
            initDriveService(account)
            Log.d(TAG, "Signed in as ${account.email}")
        } else {
            _signInState.value = SignInState.SignedOut
            Log.e(TAG, "Sign in failed")
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                com.google.android.gms.tasks.Tasks.await(googleSignInClient.signOut())
                _signInState.value = SignInState.SignedOut
                driveService = null
                Log.d(TAG, "Signed out")
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }

    private fun initDriveService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("GymTracker")
            .build()
    }

    internal fun setSyncState(state: SyncState) {
        _syncState.value = state
    }

    // --- "Oblak usvojen" --------------------------------------------------------

    /**
     * Da li je OVA instalacija već usklađena sa oblakom (vratila backup, napravila prvi
     * backup u prazan oblak, ili je korisnik izričito potvrdio). Dok nije, backup preko
     * postojećeg backup-a u oblaku traži potvrdu — sveža instalacija ne sme tiho da
     * pregazi dobar backup.
     */
    fun isCloudAdopted(): Boolean {
        if (!prefs.contains(KEY_CLOUD_ADOPTED)) {
            // Migracija postojećih instalacija: usvojeno ako je ikad uspešno sinhronizovano.
            val adopted = prefs.getLong(KEY_LAST_SYNC_TIME, 0L) > 0L
            prefs.edit().putBoolean(KEY_CLOUD_ADOPTED, adopted).apply()
            return adopted
        }
        return prefs.getBoolean(KEY_CLOUD_ADOPTED, false)
    }

    /** Izričita potvrda korisnika, uspešno vraćanje ili prvi backup u prazan oblak. */
    fun markCloudAdopted() {
        prefs.edit().putBoolean(KEY_CLOUD_ADOPTED, true).apply()
    }

    /** Uspešan backup ili vraćanje iz oblaka: instalacija je usklađena sa oblakom. */
    fun recordSuccessfulSync() {
        prefs.edit()
            .putBoolean(KEY_CLOUD_ADOPTED, true)
            .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
            .apply()
    }

    // --- Drive operacije --------------------------------------------------------

    /**
     * Izvršava blokirajući Drive poziv na IO niti i prevodi greške u [DriveException].
     * Auth i API greške se hvataju PRE IOException (obe su njegovi podtipovi).
     * CancellationException se ne hvata — otkazivanje mora da se propagira.
     */
    private suspend fun <T> driveCall(block: (Drive) -> T): T = withContext(Dispatchers.IO) {
        val drive = driveService ?: throw DriveException("Niste prijavljeni na Google Drive.")
        try {
            block(drive)
        } catch (e: UserRecoverableAuthIOException) {
            Log.e(TAG, "Potrebna ponovna autorizacija", e)
            throw DriveException(AUTH_RECOVERY_MESSAGE, e)
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Drive API greška (${e.statusCode})", e)
            throw DriveException("API greška (${e.statusCode}): ${e.statusMessage}", e)
        } catch (e: IOException) {
            Log.e(TAG, "Mrežna greška", e)
            throw DriveException("Mrežna greška: ${e.message}", e)
        }
    }

    /** Traži fajl po TAČNOM imenu. Ništa ne briše. */
    private fun findExact(drive: Drive, fileName: String): com.google.api.services.drive.model.File? {
        val result = drive.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$fileName' and trashed = false")
            .setOrderBy("modifiedTime desc")
            .setFields("files(id, name, modifiedTime)")
            .execute()
        return result.files.orEmpty().firstOrNull { it.name == fileName }
    }

    private fun listAll(drive: Drive): List<com.google.api.services.drive.model.File> {
        val all = mutableListOf<com.google.api.services.drive.model.File>()
        var pageToken: String? = null
        do {
            val result = drive.files().list()
                .setSpaces(APP_DATA_FOLDER)
                .setFields("nextPageToken, files(id, name, modifiedTime)")
                .setPageSize(1000)
                .setPageToken(pageToken)
                .execute()
            all.addAll(result.files.orEmpty())
            pageToken = result.nextPageToken
        } while (pageToken != null)
        return all
    }

    private fun download(drive: Drive, fileId: String): String {
        val output = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(output)
        return output.toString("UTF-8")
    }

    private fun prevTimestamp(name: String?): Long? =
        name?.takeIf { it.startsWith(PREV_PREFIX) && it.endsWith(PREV_SUFFIX) }
            ?.removePrefix(PREV_PREFIX)?.removeSuffix(PREV_SUFFIX)?.toLongOrNull()

    /** Da li u oblaku postoji bilo kakav backup (novi ili stari format). Greška = izuzetak. */
    suspend fun cloudBackupExists(): Boolean = driveCall { drive ->
        val names = listAll(drive).mapNotNull { it.name }.toSet()
        COMBINED_FILE_NAME in names ||
            LEGACY_WORKOUTS_FILE in names ||
            LEGACY_BODY_METRICS_FILE in names ||
            LEGACY_CYCLES_FILE in names ||
            LEGACY_CUSTOM_EXERCISES_FILE in names ||
            LEGACY_MACHINE_SETTINGS_FILE in names
    }

    /** Sadržaj kombinovanog backup-a, ili `null` ako ga (pouzdano) nema. */
    suspend fun downloadCombined(): String? = driveCall { drive ->
        findExact(drive, COMBINED_FILE_NAME)?.let { download(drive, it.id) }
    }

    /** Stari format: svih pet fajlova. `null` ako nijedan ne postoji. */
    suspend fun downloadLegacy(): LegacyCloudFiles? = driveCall { drive ->
        fun read(name: String): String? = findExact(drive, name)?.let { download(drive, it.id) }
        val files = LegacyCloudFiles(
            workouts = read(LEGACY_WORKOUTS_FILE),
            bodyMetrics = read(LEGACY_BODY_METRICS_FILE),
            cycles = read(LEGACY_CYCLES_FILE),
            customExercises = read(LEGACY_CUSTOM_EXERCISES_FILE),
            machineSettings = read(LEGACY_MACHINE_SETTINGS_FILE)
        )
        val anyPresent = listOf(
            files.workouts, files.bodyMetrics, files.cycles, files.customExercises, files.machineSettings
        ).any { it != null }
        if (anyPresent) files else null
    }

    /** Trenutni backup + prethodne kopije, najnovije prvo. */
    suspend fun listCopies(): List<CloudCopy> = driveCall { drive ->
        listAll(drive).mapNotNull { file ->
            val id = file.id ?: return@mapNotNull null
            when {
                file.name == COMBINED_FILE_NAME ->
                    CloudCopy(id, file.modifiedTime?.value ?: 0L, isCurrent = true)
                prevTimestamp(file.name) != null ->
                    CloudCopy(id, prevTimestamp(file.name) ?: 0L, isCurrent = false)
                else -> null
            }
        }.sortedWith(compareByDescending<CloudCopy> { it.isCurrent }.thenByDescending { it.createdAtMillis })
    }

    suspend fun downloadById(fileId: String): String = driveCall { drive -> download(drive, fileId) }

    /**
     * Otprema kombinovani backup. Ako glavni fajl već postoji, PRVO se pravi njegova
     * prethodna kopija; ako to ne uspe, glavni fajl se NE prepisuje.
     */
    suspend fun uploadCombined(jsonData: String) = driveCall { drive ->
        val content = ByteArrayContent.fromString("application/json", jsonData)
        val existing = findExact(drive, COMBINED_FILE_NAME)
        if (existing == null) {
            val metadata = com.google.api.services.drive.model.File().apply {
                name = COMBINED_FILE_NAME
                parents = listOf(APP_DATA_FOLDER)
            }
            drive.files().create(metadata, content).setFields("id").execute()
        } else {
            try {
                savePreviousCopy(drive, existing.id)
            } catch (e: IOException) {
                Log.e(TAG, "Prethodna kopija nije sačuvana — backup se prekida", e)
                throw DriveException(
                    "Prethodna kopija backup-a nije mogla da se sačuva, pa postojeći backup NIJE prepisan (${e.message}).",
                    e
                )
            }
            drive.files().update(existing.id, null, content).execute()
            prunePreviousCopies(drive)
        }
        Log.d(TAG, "Backup uspešan: $COMBINED_FILE_NAME")
    }

    private fun savePreviousCopy(drive: Drive, fileId: String) {
        val metadata = com.google.api.services.drive.model.File().apply {
            name = "$PREV_PREFIX${System.currentTimeMillis()}$PREV_SUFFIX"
            parents = listOf(APP_DATA_FOLDER)
        }
        try {
            drive.files().copy(fileId, metadata).setFields("id").execute()
        } catch (e: GoogleJsonResponseException) {
            // Ako Drive odbije copy u appDataFolder-u, kopija se pravi ručno: preuzmi pa kreiraj.
            Log.w(TAG, "files.copy odbijen (${e.statusCode}), pravim kopiju ručno", e)
            val previous = download(drive, fileId)
            drive.files()
                .create(metadata, ByteArrayContent.fromString("application/json", previous))
                .setFields("id")
                .execute()
        }
    }

    /** Čuva najnovijih [PREV_COPIES_TO_KEEP] prethodnih kopija. Neuspelo brisanje nije greška backup-a. */
    private fun prunePreviousCopies(drive: Drive) {
        try {
            listAll(drive)
                .mapNotNull { file -> prevTimestamp(file.name)?.let { it to file } }
                .sortedByDescending { it.first }
                .drop(PREV_COPIES_TO_KEEP)
                .forEach { (_, file) ->
                    try {
                        drive.files().delete(file.id).execute()
                        Log.d(TAG, "Obrisana stara prethodna kopija: ${file.name}")
                    } catch (e: IOException) {
                        Log.w(TAG, "Neuspešno brisanje stare kopije: ${file.name}", e)
                    }
                }
        } catch (e: IOException) {
            Log.w(TAG, "Čišćenje starih kopija nije uspelo", e)
        }
    }

    fun isSignedIn(): Boolean = _signInState.value is SignInState.SignedIn

    fun isFirstLaunch(): Boolean = !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }
}

sealed class SignInState {
    data object SignedOut : SignInState()
    data class SignedIn(val email: String) : SignInState()
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
