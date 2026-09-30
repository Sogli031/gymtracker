# Podešavanja mašina — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Omogućiti korisnicima da za svaku vežbu sačuvaju podešavanja mašine (visina sedišta, stopper, pad za noge itd.) koja se pamte i prikazuju pri svakom narednom treningu.

**Architecture:** Podešavanja mašina su **globalna po imenu vežbe** — kad jednom sačuvaš "Chest Press Machine → sedište: 5, stopper: 3", to se prikazuje svaki put kad dodaš tu vežbu u trening. Čuva se u zasebnom JSON fajlu (`machine_settings.json`) sa novim `MachineSettingsRepository` koji prati isti pattern kao ostali repozitorijumi (StateFlow, lokalno + cloud sync). U UI-u se podešavanja prikazuju u `ExerciseCard`-u ispod naziva vežbe, sa gear ikonom za uređivanje.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Gson, Google Drive API (appDataFolder)

---

## Task 1: Data model — MachineSettings.kt

**Files:**
- Create: `app/src/main/java/com/gymtracker/app/data/model/MachineSettings.kt`

**Step 1: Kreiraj data model fajl**

```kotlin
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
```

**Step 2: Verifikuj**

Fajl treba da se nalazi na tačnoj putanji i da se kompajlira sa ostatkom projekta. Nema zavisnosti od drugih novih fajlova.

---

## Task 2: Config — dodaj putanju za machine_settings.json

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/data/model/Config.kt`

**Step 1: Dodaj konstantu za fajl**

U `Config` object dodaj:
```kotlin
const val MACHINE_SETTINGS_FILE = "machine_settings.json"
```

Dodaj odmah ispod `CYCLES_FILE` linije.

---

## Task 3: Repository — MachineSettingsRepository.kt

**Files:**
- Create: `app/src/main/java/com/gymtracker/app/data/repository/MachineSettingsRepository.kt`

**Step 1: Kreiraj repozitorijum**

Prati isti pattern kao `CustomExerciseRepository` — StateFlow, lokalno čuvanje, cloud sync.

```kotlin
package com.gymtracker.app.data.repository

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import com.gymtracker.app.data.backup.BackupProvider
import com.gymtracker.app.data.model.Config
import com.gymtracker.app.data.model.MachineSettings
import com.gymtracker.app.util.GsonProvider
import com.gymtracker.app.util.JsonFileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

private const val TAG = "MachineSettingsRepo"

class MachineSettingsRepository(private val app: Application) {
    private val file = File(app.filesDir, Config.MACHINE_SETTINGS_FILE)
    private val gson: Gson = GsonProvider.instance
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileMutex = Mutex()
    private val cloudMutex = Mutex()

    private val _settings = MutableStateFlow<List<MachineSettings>>(emptyList())
    val settings: StateFlow<List<MachineSettings>> = _settings

    init {
        ioScope.launch {
            load()
            syncWithCloud()
        }
    }

    private suspend fun load() = fileMutex.withLock {
        val items = JsonFileStorage.loadListFromFile<MachineSettings>(file, gson)
        if (items.isNotEmpty()) {
            _settings.value = items.sortedBy { it.exerciseName.lowercase() }
            Log.d(TAG, "Loaded ${items.size} machine settings")
        }
    }

    private suspend fun save() = fileMutex.withLock {
        try {
            JsonFileStorage.saveToFile(file, gson.toJson(_settings.value))
            Log.d(TAG, "Saved ${_settings.value.size} machine settings")
            saveToCloud()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save machine settings", e)
        }
    }

    private suspend fun saveToCloud() {
        val backup = BackupProvider.get(app)
        if (!backup.isSignedIn()) return

        try {
            val data = gson.toJson(_settings.value)
            backup.backupMachineSettings(data)
            Log.d(TAG, "Saved machine settings to cloud")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save machine settings to cloud", e)
        }
    }

    private suspend fun syncWithCloud() {
        val backup = BackupProvider.get(app)
        if (!backup.isSignedIn()) {
            Log.d(TAG, "Not signed in, skipping cloud sync for machine settings")
            return
        }

        if (_settings.value.isEmpty()) {
            loadFromCloud()
        }
    }

    suspend fun syncAfterSignIn() {
        if (_settings.value.isNotEmpty()) {
            saveToCloud()
        } else {
            loadFromCloud()
        }
    }

    suspend fun loadFromCloud() = cloudMutex.withLock {
        val backup = BackupProvider.get(app)
        if (!backup.isSignedIn()) {
            Log.d(TAG, "Not signed in, skipping cloud load for machine settings")
            return@withLock
        }

        try {
            val data = backup.restoreMachineSettings()
            if (data != null && data.isNotBlank()) {
                val items = gson.fromJson(data, Array<MachineSettings>::class.java)?.toList().orEmpty()
                _settings.value = items.sortedBy { it.exerciseName.lowercase() }
                JsonFileStorage.saveToFile(file, data)
                Log.d(TAG, "Loaded ${items.size} machine settings from cloud")
            } else {
                Log.d(TAG, "No cloud data found for machine settings")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load machine settings from cloud", e)
        }
    }

    fun getDataAsJson(): String = gson.toJson(_settings.value)

    /**
     * Vraća podešavanja mašine za datu vežbu (case-insensitive).
     */
    fun getForExercise(exerciseName: String): MachineSettings? {
        return _settings.value.firstOrNull {
            it.exerciseName.equals(exerciseName, ignoreCase = true)
        }
    }

    /**
     * Čuva ili ažurira podešavanja mašine za vežbu.
     * Ako su settings prazni, briše unos.
     */
    fun save(machineSettings: MachineSettings) {
        if (machineSettings.settings.isEmpty()) {
            // Obriši ako nema podešavanja
            _settings.update { list ->
                list.filter { !it.exerciseName.equals(machineSettings.exerciseName, ignoreCase = true) }
            }
        } else {
            _settings.update { list ->
                val existing = list.indexOfFirst {
                    it.exerciseName.equals(machineSettings.exerciseName, ignoreCase = true)
                }
                if (existing >= 0) {
                    list.toMutableList().apply { set(existing, machineSettings) }
                        .sortedBy { it.exerciseName.lowercase() }
                } else {
                    (list + machineSettings).sortedBy { it.exerciseName.lowercase() }
                }
            }
        }
        ioScope.launch { save() }
    }
}

object MachineSettingsRepositoryProvider {
    @Volatile
    private var instance: MachineSettingsRepository? = null

    fun get(app: Application): MachineSettingsRepository {
        return instance ?: synchronized(this) {
            instance ?: MachineSettingsRepository(app).also { instance = it }
        }
    }
}
```

---

## Task 4: Google Drive backup — dodaj machine settings metode

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/data/backup/GoogleDriveBackup.kt`

**Step 1: Dodaj konstantu za ime fajla**

Na vrhu fajla (gde su ostale konstante), dodaj:
```kotlin
private const val MACHINE_SETTINGS_FILE_NAME = "gymtracker_machine_settings.json"
```

**Step 2: Dodaj find metodu**

Ispod `findCustomExercisesFile`, dodaj:
```kotlin
private fun findMachineSettingsFile(drive: Drive): String? {
    val result = drive.files().list()
        .setSpaces(APP_DATA_FOLDER)
        .setQ("name = '$MACHINE_SETTINGS_FILE_NAME' and trashed = false")
        .setOrderBy("modifiedTime desc")
        .setFields("files(id, name)")
        .execute()
    return result.files?.firstOrNull()?.id
}
```

**Step 3: Dodaj backup metodu**

```kotlin
suspend fun backupMachineSettings(jsonData: String): Boolean {
    val drive = driveService ?: return false

    if (jsonData.isBlank()) {
        Log.w(TAG, "Machine settings backup skipped - empty data")
        return false
    }

    return withContext(Dispatchers.IO) {
        try {
            val existingFileId = findMachineSettingsFile(drive)
            val contentStream = ByteArrayContent.fromString("application/json", jsonData)

            if (existingFileId != null) {
                drive.files().update(existingFileId, null, contentStream).execute()
                Log.d(TAG, "Machine settings backup updated")
            } else {
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = MACHINE_SETTINGS_FILE_NAME
                    parents = listOf(APP_DATA_FOLDER)
                }
                drive.files().create(fileMetadata, contentStream).execute()
                Log.d(TAG, "Machine settings backup created")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Machine settings backup failed", e)
            false
        }
    }
}
```

**Step 4: Dodaj restore metodu**

```kotlin
suspend fun restoreMachineSettings(): String? {
    val drive = driveService ?: return null

    return withContext(Dispatchers.IO) {
        try {
            val fileId = findMachineSettingsFile(drive)
            if (fileId == null) {
                Log.d(TAG, "No machine settings backup found")
                return@withContext null
            }

            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            val data = outputStream.toString("UTF-8")
            Log.d(TAG, "Machine settings restore completed")
            data
        } catch (e: Exception) {
            Log.e(TAG, "Machine settings restore failed", e)
            null
        }
    }
}
```

---

## Task 5: App wiring — inicijalizacija i provlačenje repozitorijuma

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/GymTrackerApplication.kt`
- Modify: `app/src/main/java/com/gymtracker/app/MainActivity.kt`
- Modify: `app/src/main/java/com/gymtracker/app/ui/screens/MainScreen.kt`
- Modify: `app/src/main/java/com/gymtracker/app/ui/screens/SettingsScreen.kt`

**Step 1: GymTrackerApplication — dodaj inicijalizaciju**

U `onCreate()` dodaj ispod `CustomExerciseRepositoryProvider.get(this)`:
```kotlin
MachineSettingsRepositoryProvider.get(this)
```

I dodaj import:
```kotlin
import com.gymtracker.app.data.repository.MachineSettingsRepositoryProvider
```

**Step 2: MainActivity — dodaj repozitorijum i prosledi ga**

U `onCreate()`, ispod `val backup = ...` dodaj:
```kotlin
val machineSettingsRepository = MachineSettingsRepositoryProvider.get(application)
```

Prosledi ga u `AppNavigation`:
```kotlin
AppNavigation(workoutRepository, customExerciseRepository, bodyMetricsRepository, cycleRepository, backup, machineSettingsRepository)
```

Ažuriraj potpis `AppNavigation`:
```kotlin
@Composable
private fun AppNavigation(
    workoutRepository: WorkoutRepository,
    customExerciseRepository: CustomExerciseRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    cycleRepository: CycleRepository,
    backup: GoogleDriveBackup,
    machineSettingsRepository: MachineSettingsRepository
)
```

Prosledi `machineSettingsRepository` u `MainScreen` i u `AddWorkoutScreen` (sva 3 poziva — add, edit, repeat):
```kotlin
machineSettingsRepository = machineSettingsRepository
```

Dodaj import:
```kotlin
import com.gymtracker.app.data.repository.MachineSettingsRepository
import com.gymtracker.app.data.repository.MachineSettingsRepositoryProvider
```

**Step 3: MainScreen — prosledi dalje**

Dodaj parametar u `MainScreen`:
```kotlin
machineSettingsRepository: MachineSettingsRepository,
```

Prosledi u `SettingsContent`:
```kotlin
machineSettingsRepository = machineSettingsRepository,
```

Nemoj ga prosleđivati u `LogContent` ili `StatisticsContent` — nije im potreban.

**Step 4: AddWorkoutScreen — dodaj parametar**

Dodaj parametar:
```kotlin
machineSettingsRepository: MachineSettingsRepository? = null,
```

(Ovo se koristi u Task 7.)

---

## Task 6: Cloud sync — dodaj machine settings u sync tokove

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/gymtracker/app/ui/screens/MainScreen.kt`

**Step 1: SettingsScreen — dodaj machineSettingsRepository parametar**

Ažuriraj potpis `SettingsContent`:
```kotlin
fun SettingsContent(
    backup: GoogleDriveBackup,
    repository: WorkoutRepository,
    customExerciseRepository: CustomExerciseRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    cycleRepository: CycleRepository,
    machineSettingsRepository: MachineSettingsRepository,
    modifier: Modifier = Modifier
)
```

**Step 2: Dodaj sync u sign-in flow**

U `signInLauncher` callback-u, dodaj nakon `customExerciseRepository.syncAfterSignIn()`:
```kotlin
machineSettingsRepository.syncAfterSignIn()
```

**Step 3: Dodaj sync u "Osveži sa cloud-a" dijalog**

U `showRestoreConfirmDialog` confirmButton onClick, dodaj:
```kotlin
machineSettingsRepository.loadFromCloud()
```

**Step 4: Dodaj backup u "Sačuvaj na cloud" dugme**

U ručni backup onClick, dodaj:
```kotlin
backup.backupMachineSettings(machineSettingsRepository.getDataAsJson())
```

**Step 5: Ažuriraj MainScreen sign-in flow**

U `MainScreen` signInLauncher callback, dodaj nakon `customExerciseRepository.syncAfterSignIn()`:
```kotlin
machineSettingsRepository.syncAfterSignIn()
```

Dodaj import:
```kotlin
import com.gymtracker.app.data.repository.MachineSettingsRepository
```

---

## Task 7: UI — MachineSettingsDialog composable

**Files:**
- Create: `app/src/main/java/com/gymtracker/app/ui/components/MachineSettingsDialog.kt`

**Step 1: Kreiraj dijalog za uređivanje podešavanja mašine**

```kotlin
package com.gymtracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymtracker.app.data.model.MachineSetting
import com.gymtracker.app.ui.theme.UiTokens

@Composable
fun MachineSettingsDialog(
    exerciseName: String,
    currentSettings: List<MachineSetting>,
    onDismiss: () -> Unit,
    onSave: (List<MachineSetting>) -> Unit
) {
    // Mutable lista za editovanje — inicijalizuj sa postojećim + prazan red ako nema nijednog
    val editableSettings = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            if (currentSettings.isNotEmpty()) {
                addAll(currentSettings.map { it.name to it.value })
            } else {
                add("" to "")
            }
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = UiTokens.Accent,
        unfocusedBorderColor = UiTokens.GlassBorder,
        focusedTextColor = UiTokens.TextPrimary,
        unfocusedTextColor = UiTokens.TextPrimary,
        cursorColor = UiTokens.Accent,
        focusedContainerColor = UiTokens.SurfaceElevated,
        unfocusedContainerColor = UiTokens.SurfaceElevated
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Podešavanja mašine") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UiTokens.TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                editableSettings.forEachIndexed { index, (name, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                editableSettings[index] = newName to editableSettings[index].second
                            },
                            placeholder = { Text("Naziv", color = UiTokens.TextMuted.copy(alpha = 0.5f)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = textFieldColors
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { newValue ->
                                editableSettings[index] = editableSettings[index].first to newValue
                            },
                            placeholder = { Text("Vrednost", color = UiTokens.TextMuted.copy(alpha = 0.5f)) },
                            modifier = Modifier.weight(0.6f),
                            singleLine = true,
                            colors = textFieldColors
                        )
                        if (editableSettings.size > 1) {
                            IconButton(
                                onClick = { editableSettings.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Obriši",
                                    tint = UiTokens.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Dugme za dodavanje novog podešavanja
                TextButton(
                    onClick = { editableSettings.add("" to "") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = UiTokens.Accent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dodaj podešavanje",
                        color = UiTokens.Accent
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Filtriraj prazne redove i sačuvaj
                    val result = editableSettings
                        .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                        .map { MachineSetting(name = it.first.trim(), value = it.second.trim()) }
                    onSave(result)
                }
            ) {
                Text("Sačuvaj", color = UiTokens.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        },
        containerColor = UiTokens.Surface,
        titleContentColor = UiTokens.TextPrimary,
        textContentColor = UiTokens.TextSecondary
    )
}
```

---

## Task 8: UI — Prikaži podešavanja mašine u ExerciseCard

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/ui/components/ExerciseCard.kt`

**Step 1: Dodaj parametre za machine settings**

U potpis `ExerciseCard` dodaj dva nova parametra pre `modifier`:
```kotlin
machineSettings: List<MachineSetting> = emptyList(),
onEditMachineSettings: (() -> Unit)? = null,
```

Dodaj import:
```kotlin
import com.gymtracker.app.data.model.MachineSetting
```

**Step 2: Dodaj gear ikonu u header**

U header `Row`, ispred replace dugmeta (pre `if (onReplaceExercise != null)`), dodaj:
```kotlin
if (onEditMachineSettings != null) {
    IconButton(onClick = onEditMachineSettings, modifier = Modifier.size(36.dp)) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Podešavanja mašine",
            tint = if (machineSettings.isNotEmpty()) UiTokens.Accent else UiTokens.TextMuted
        )
    }
}
```

Dodaj import:
```kotlin
import androidx.compose.material.icons.filled.Settings
```

**Step 3: Prikaži podešavanja ispod header-a**

Posle header `Row` i pre `Spacer(modifier = Modifier.height(12.dp))`, dodaj:
```kotlin
if (machineSettings.isNotEmpty()) {
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UiTokens.SurfaceElevated, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        machineSettings.forEach { setting ->
            Text(
                text = "${setting.name}: ${setting.value}",
                style = MaterialTheme.typography.labelSmall,
                color = UiTokens.TextMuted
            )
        }
    }
}
```

Dodaj importe:
```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
```

(Napomena: `Alignment` je možda već importovan, a `RoundedCornerShape` takođe. Proveri pre dodavanja duplikata.)

---

## Task 9: UI — Integriši u AddWorkoutScreen

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/ui/screens/AddWorkoutScreen.kt`

**Step 1: Dodaj state za machine settings dijalog**

Ispod `var exerciseToDelete ...` dodaj:
```kotlin
var exerciseForMachineSettings by remember { mutableStateOf<WorkoutExercise?>(null) }
```

**Step 2: Učitaj machine settings iz repozitorijuma**

Ispod bloka `val placeholders = remember(exercises) { ... }` dodaj:
```kotlin
val allMachineSettings by (machineSettingsRepository?.settings
    ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

val machineSettingsMap = remember(allMachineSettings, exercises) {
    exercises.associate { exercise ->
        exercise.exerciseName to (allMachineSettings.firstOrNull {
            it.exerciseName.equals(exercise.exerciseName, ignoreCase = true)
        }?.settings ?: emptyList())
    }
}
```

Dodaj import:
```kotlin
import com.gymtracker.app.data.model.MachineSetting
import com.gymtracker.app.data.model.MachineSettings
import com.gymtracker.app.data.repository.MachineSettingsRepository
import com.gymtracker.app.ui.components.MachineSettingsDialog
```

**Step 3: Prosledi machine settings u ExerciseCard**

U `ExerciseCard` poziv unutar `items(...)` bloka, dodaj parametre:
```kotlin
machineSettings = machineSettingsMap[exercise.exerciseName] ?: emptyList(),
onEditMachineSettings = {
    exerciseForMachineSettings = exercise
},
```

**Step 4: Dodaj MachineSettingsDialog**

Na kraju composable-a (pre poslednje zagrade `AddWorkoutScreen`-a), posle `exerciseToDelete?.let { ... }` bloka, dodaj:
```kotlin
// Machine settings dialog
exerciseForMachineSettings?.let { exercise ->
    MachineSettingsDialog(
        exerciseName = exercise.exerciseName,
        currentSettings = machineSettingsMap[exercise.exerciseName] ?: emptyList(),
        onDismiss = { exerciseForMachineSettings = null },
        onSave = { newSettings ->
            machineSettingsRepository?.save(
                MachineSettings(
                    exerciseName = exercise.exerciseName,
                    settings = newSettings
                )
            )
            exerciseForMachineSettings = null
        }
    )
}
```

---

## Task 10: Finalno provlačenje — prosledi repozitorijum u AddWorkoutScreen iz MainActivity

**Files:**
- Modify: `app/src/main/java/com/gymtracker/app/MainActivity.kt`

**Step 1: Prosledi machineSettingsRepository u sva tri composable poziva**

U sva tri `AddWorkoutScreen(...)` poziva (add_workout, edit_workout, repeat_workout) dodaj:
```kotlin
machineSettingsRepository = machineSettingsRepository
```

Verifikuj da su `add_workout`, `edit_workout/{workoutId}` i `repeat_workout/{copyFromWorkoutId}` rute ažurirane.

---

## Rezime izmena po fajlu

| Fajl | Akcija |
|------|--------|
| `data/model/MachineSettings.kt` | **Novi** — MachineSetting + MachineSettings data klase |
| `data/model/Config.kt` | Dodaj `MACHINE_SETTINGS_FILE` konstantu |
| `data/repository/MachineSettingsRepository.kt` | **Novi** — repozitorijum sa StateFlow + cloud sync |
| `data/backup/GoogleDriveBackup.kt` | Dodaj find/backup/restore metode za machine settings |
| `GymTrackerApplication.kt` | Inicijalizuj MachineSettingsRepositoryProvider |
| `MainActivity.kt` | Kreiraj i prosledi machineSettingsRepository |
| `ui/screens/MainScreen.kt` | Prosledi machineSettingsRepository |
| `ui/screens/SettingsScreen.kt` | Dodaj sync za machine settings |
| `ui/components/MachineSettingsDialog.kt` | **Novi** — dijalog za uređivanje podešavanja |
| `ui/components/ExerciseCard.kt` | Dodaj gear ikonu + prikaz podešavanja |
| `ui/screens/AddWorkoutScreen.kt` | Integriši machine settings (state, map, dijalog) |

**Nema novih biblioteka/zavisnosti.** Koristi se postojeći Gson, Drive API i Compose.
