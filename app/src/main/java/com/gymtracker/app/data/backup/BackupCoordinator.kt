package com.gymtracker.app.data.backup

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "BackupCoordinator"

/** Pitanje na koje korisnik mora da odgovori pre nego što se operacija nastavi. */
sealed class BackupPrompt {
    data object None : BackupPrompt()

    /** Odmah posle prijave: u oblaku postoji backup, a ova instalacija ga još nije preuzela. */
    data object OfferRestoreAfterSignIn : BackupPrompt()

    /** Ručni backup, a oblak još nije "usvojen": ne otpremaj tiho preko postojećeg backup-a. */
    data class CloudNotAdopted(val localIsEmpty: Boolean) : BackupPrompt()

    /** Lokalno nema podataka, a u oblaku postoji backup. */
    data object EmptyLocalOverCloud : BackupPrompt()

    /** Backup je prazan, a lokalno postoje podaci. */
    data class EmptyBackupOverLocal(val localCount: Int) : BackupPrompt()
}

data class BackupMessage(val success: Boolean, val title: String, val text: String)

/**
 * Vodi backup i vraćanje od početka do kraja, u korutini na nivou aplikacije —
 * promena taba ili zatvaranje ekrana NE prekida posao (ranije je rememberCoroutineScope
 * otkazivao preostale fajlove, pa se vraćalo "pola podataka").
 *
 * Pravila:
 *  - "prazno ne gazi popunjeno" u oba smera, bez izričite potvrde;
 *  - neuspela provera oblaka nikad se ne tumači kao "oblak je prazan";
 *  - vraćanje je sve-ili-ništa; uspeh se javlja tek kad je SVE završeno;
 *  - CancellationException se nikad ne guta.
 */
class BackupCoordinator(
    private val drive: GoogleDriveBackup,
    private val store: BackupStore,
    private val onDataReplaced: () -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val _prompt = MutableStateFlow<BackupPrompt>(BackupPrompt.None)
    val prompt: StateFlow<BackupPrompt> = _prompt

    private val _message = MutableStateFlow<BackupMessage?>(null)
    val message: StateFlow<BackupMessage?> = _message

    /** `null` = lista nije tražena; prazna lista = u oblaku nema kopija. */
    private val _copies = MutableStateFlow<List<CloudCopy>?>(null)
    val copies: StateFlow<List<CloudCopy>?> = _copies

    /** Dokument koji čeka potvrdu (prazan backup preko popunjenih lokalnih podataka). */
    private var pendingRestore: Pair<BackupDocument, Boolean>? = null

    fun dismissPrompt() {
        _prompt.value = BackupPrompt.None
        pendingRestore = null
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun dismissCopies() {
        _copies.value = null
    }

    /** Pokreće posao; greške pretvara u poruku, a otkazivanje propušta dalje. */
    private fun launchOperation(failureTitle: String, block: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                drive.setSyncState(SyncState.Syncing)
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, failureTitle, e)
                    val text = e.message ?: e.javaClass.simpleName
                    drive.setSyncState(SyncState.Error(text))
                    _message.value = BackupMessage(false, failureTitle, text)
                }
            }
        }
    }

    /** Posle uspešne prijave: ako oblak nije usvojen, a backup postoji — ponudi vraćanje. */
    fun onSignedIn() {
        if (drive.isCloudAdopted()) return
        scope.launch {
            try {
                if (drive.cloudBackupExists()) {
                    _prompt.value = BackupPrompt.OfferRestoreAfterSignIn
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Neuspela provera NE znači da je oblak prazan: zaštita ostaje uključena,
                // a ista provera se ponavlja pri svakom pokušaju backup-a.
                Log.w(TAG, "Provera oblaka posle prijave nije uspela", e)
                _message.value = BackupMessage(
                    false,
                    "Provera oblaka nije uspela",
                    "Nije moglo da se proveri da li u oblaku već postoji backup (${e.message}). " +
                        "Pre pravljenja novog backup-a pokušaj „Vrati iz oblaka“."
                )
            }
        }
    }

    /** Ručni backup. [confirmed] = korisnik je izričito potvrdio prepisivanje. */
    fun backup(confirmed: Boolean = false) {
        _prompt.value = BackupPrompt.None
        launchOperation("Backup nije uspeo") {
            val document = store.export()
            // Baca izuzetak ako provera ne uspe — tada se NIŠTA ne otprema.
            val cloudExists = drive.cloudBackupExists()

            if (cloudExists && !confirmed) {
                if (!drive.isCloudAdopted()) {
                    drive.setSyncState(SyncState.Idle)
                    _prompt.value = BackupPrompt.CloudNotAdopted(localIsEmpty = document.isEmpty)
                    return@launchOperation
                }
                if (document.isEmpty) {
                    drive.setSyncState(SyncState.Idle)
                    _prompt.value = BackupPrompt.EmptyLocalOverCloud
                    return@launchOperation
                }
            }

            drive.uploadCombined(BackupCodec.encode(document))
            drive.recordSuccessfulSync()
            drive.setSyncState(SyncState.Success)
            _message.value = BackupMessage(
                true,
                "Backup je sačuvan",
                "Sačuvano u oblaku — ${document.summary()}."
            )
        }
    }

    /** "Ipak napravi backup": korisnik izričito usvaja trenutno lokalno stanje. */
    fun backupAnyway() {
        drive.markCloudAdopted()
        backup(confirmed = true)
    }

    /** Vraćanje iz oblaka; [copy] = određena (prethodna) kopija, inače trenutni backup. */
    fun restore(copy: CloudCopy? = null) {
        _prompt.value = BackupPrompt.None
        _copies.value = null
        launchOperation("Vraćanje nije uspelo") {
            // Sve se preuzima, parsira i proverava PRE prvog upisa u bazu.
            val document = when {
                copy != null -> BackupCodec.decode(drive.downloadById(copy.fileId))
                else -> drive.downloadCombined()?.let { BackupCodec.decode(it) }
                    ?: drive.downloadLegacy()?.let {
                        BackupCodec.decodeLegacy(
                            it.workouts, it.bodyMetrics, it.cycles, it.customExercises, it.machineSettings
                        )
                    }
            }
            if (document == null) {
                drive.setSyncState(SyncState.Idle)
                _message.value = BackupMessage(false, "Nema backup-a", "U oblaku nema nijednog backup-a za ovaj nalog.")
                return@launchOperation
            }
            applyOrAskIfEmpty(document, fromCloud = true)
        }
    }

    /** Uvoz iz lokalnog fajla — isti dokument i ista pravila kao vraćanje iz oblaka. */
    fun importFromJson(json: String) {
        _prompt.value = BackupPrompt.None
        launchOperation("Uvoz nije uspeo") {
            applyOrAskIfEmpty(BackupCodec.decode(json), fromCloud = false)
        }
    }

    /** Potvrda da prazan backup sme da zameni popunjene lokalne podatke. */
    fun confirmPendingRestore() {
        val pending = pendingRestore ?: return
        pendingRestore = null
        _prompt.value = BackupPrompt.None
        launchOperation("Vraćanje nije uspelo") {
            apply(pending.first, pending.second)
        }
    }

    private suspend fun applyOrAskIfEmpty(document: BackupDocument, fromCloud: Boolean) {
        if (document.isEmpty) {
            val localCount = store.localRecordCount()
            if (localCount > 0) {
                pendingRestore = document to fromCloud
                drive.setSyncState(SyncState.Idle)
                _prompt.value = BackupPrompt.EmptyBackupOverLocal(localCount)
                return
            }
        }
        apply(document, fromCloud)
    }

    private suspend fun apply(document: BackupDocument, fromCloud: Boolean) {
        // Vraćanje prepisuje ceo sadržaj baze; bez ovoga bi ti upisi odmah zakazali
        // auto-backup tog istog stanja nazad u oblak.
        BackupScheduler.suppressAfterChange = true
        try {
            store.replaceAll(document)
        } finally {
            BackupScheduler.suppressAfterChange = false
        }
        onDataReplaced()
        if (fromCloud) drive.recordSuccessfulSync()
        drive.setSyncState(if (fromCloud) SyncState.Success else SyncState.Idle)

        val missing = document.missingSections()
        val text = buildString {
            append("Vraćeno — ${document.summary()}.")
            if (missing.isNotEmpty()) {
                append("\n\nU backup-u nije bilo (lokalno ostalo nepromenjeno): ${missing.joinToString(", ")}.")
            }
        }
        _message.value = BackupMessage(true, "Podaci su vraćeni", text)
    }

    /** Kombinovani dokument za izvoz u lokalni fajl. */
    suspend fun exportToJson(): String = BackupCodec.encode(store.export())

    /** Učitava listu kopija u oblaku (trenutna + prethodne) za izbor starije verzije. */
    fun loadCopies() {
        launchOperation("Lista kopija nije učitana") {
            val found = drive.listCopies()
            drive.setSyncState(SyncState.Idle)
            if (found.isEmpty()) {
                _message.value = BackupMessage(false, "Nema kopija", "U oblaku nema sačuvanih kopija backup-a.")
            } else {
                _copies.value = found
            }
        }
    }
}
