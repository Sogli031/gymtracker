package com.gymtracker.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.gymtracker.app.data.backup.BackupCoordinator
import com.gymtracker.app.data.backup.GoogleDriveBackup
import com.gymtracker.app.data.backup.SignInState
import com.gymtracker.app.data.backup.SyncState
import com.gymtracker.app.data.model.CustomExercise
import com.gymtracker.app.data.model.ExerciseCategory
import com.gymtracker.app.data.repository.BodyMetricsRepository
import com.gymtracker.app.data.repository.CustomExerciseRepository
import com.gymtracker.app.data.repository.CycleRepository
import com.gymtracker.app.data.repository.MachineSettingsRepository
import com.gymtracker.app.data.repository.WorkoutRepository
import com.gymtracker.app.util.pluralize
import com.gymtracker.app.ui.theme.CardShape
import com.gymtracker.app.ui.theme.UiTokens
import com.gymtracker.app.ui.theme.categoryColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    backup: GoogleDriveBackup,
    coordinator: BackupCoordinator,
    repository: WorkoutRepository,
    customExerciseRepository: CustomExerciseRepository,
    bodyMetricsRepository: BodyMetricsRepository,
    cycleRepository: CycleRepository,
    machineSettingsRepository: MachineSettingsRepository,
    modifier: Modifier = Modifier
) {
    val signInState by backup.signInState.collectAsState()
    val syncState by backup.syncState.collectAsState()
    val customExercises by customExerciseRepository.exercises.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddExerciseDialog by rememberSaveable { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<CustomExercise?>(null) }
    var showRestoreConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var showImportConfirmDialog by rememberSaveable { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    // Isti kombinovani dokument kao za backup u oblaku (sve kategorije).
                    val json = coordinator.exportToJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(json.toByteArray(Charsets.UTF_8))
                        } ?: throw java.io.IOException("Fajl ne može da se otvori za upis")
                    }
                    snackbarHostState.showSnackbar("Izvoz uspešan")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Greška pri izvozu: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                if (json != null) {
                    pendingImportJson = json
                    showImportConfirmDialog = true
                }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Greška pri čitanju fajla: ${e.message}") }
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                backup.handleSignInResult(account)
                // Ako u oblaku već postoji backup, odmah ponudi vraćanje.
                if (account != null) coordinator.onSignedIn()
            } catch (e: Exception) {
                backup.handleSignInResult(null)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            TopAppBar(title = { Text("Podešavanja") })

            // Google Backup Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (signInState) {
                                is SignInState.SignedIn -> Icons.Default.Cloud
                                is SignInState.SignedOut -> Icons.Default.CloudOff
                            },
                            contentDescription = null,
                            tint = when (signInState) {
                                is SignInState.SignedIn -> UiTokens.Success
                                is SignInState.SignedOut -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Drive Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (signInState) {
                                    is SignInState.SignedIn -> "Povezano: ${(signInState as SignInState.SignedIn).email}"
                                    is SignInState.SignedOut -> "Nije povezano"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sync status
                    when (syncState) {
                        is SyncState.Syncing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sinhronizacija...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is SyncState.Success -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = UiTokens.Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sinhronizovano",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UiTokens.Success
                                )
                            }
                        }
                        is SyncState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Greška: ${(syncState as SyncState.Error).message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (signInState) {
                        is SignInState.SignedOut -> {
                            Button(
                                onClick = {
                                    signInLauncher.launch(backup.getSignInIntent())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Prijavi se sa Google")
                            }
                        }
                        is SignInState.SignedIn -> {
                            OutlinedButton(
                                onClick = { showRestoreConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = syncState !is SyncState.Syncing
                            ) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vrati iz oblaka")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Ručni backup: koordinator proverava oblak i traži potvrdu
                            // pre nego što bi prazno/novo stanje pregazilo postojeći backup.
                            OutlinedButton(
                                onClick = { coordinator.backup() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = syncState !is SyncState.Syncing
                            ) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sačuvaj u oblak")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { coordinator.loadCopies() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = syncState !is SyncState.Syncing
                            ) {
                                Text("Vrati stariju kopiju", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(
                                onClick = { showSignOutConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Odjavi se",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "O backup-u",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Backup i vraćanje su potpuno ručni\n" +
                               "• „Sačuvaj u oblak“ čuva SVE podatke u jednom fajlu\n" +
                               "• „Vrati iz oblaka“ zamenjuje podatke na telefonu\n" +
                               "• Pre svakog prepisivanja čuva se prethodna kopija (poslednjih 5)\n" +
                               "• Ne troši tvoj Google Drive storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Export/Import Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Izvoz i uvoz podataka",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sačuvaj ili učitaj sve podatke iz JSON fajla",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            exportLauncher.launch("gymtracker_export.json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Izvezi podatke")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uvezi podatke")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Exercises Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = CardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Moje vežbe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (customExercises.isEmpty()) "Dodaj svoje vežbe"
                                       else "${customExercises.size} ${pluralize(customExercises.size, "vežba", "vežbe", "vežbi")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showAddExerciseDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Dodaj vežbu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (customExercises.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        customExercises.forEach { exercise ->
                            val exerciseColor = if (exercise.isCompound) UiTokens.Compound else UiTokens.Isolation
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(exerciseColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = exerciseColor
                                    )
                                    Text(
                                        text = "${exercise.category.displayName} • ${if (exercise.isCompound) "Složena" else "Izolacija"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { exerciseToDelete = exercise },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Obriši",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Add Exercise Dialog
    if (showAddExerciseDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, category, isCompound ->
                customExerciseRepository.add(
                    CustomExercise(
                        name = name,
                        category = category,
                        isCompound = isCompound
                    )
                )
                showAddExerciseDialog = false
            }
        )
    }

    // Delete Exercise Dialog
    exerciseToDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Obriši vežbu?") },
            text = { Text("Da li želiš da obrišeš \"${exercise.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        customExerciseRepository.delete(exercise.id)
                        exerciseToDelete = null
                    }
                ) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Odustani")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Vrati iz oblaka?") },
            text = {
                Column {
                    Text(
                        text = "Ovo će zameniti trenutne lokalne podatke podacima iz oblaka.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ako imaš nesačuvane lokalne izmene, one će biti izgubljene.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UiTokens.Warning
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        // Radi u korutini aplikacije; rezultat stiže kao dijalog sa rezimeom.
                        coordinator.restore()
                    }
                ) {
                    Text("Vrati", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Odustani")
                }
            }
        )
    }

    // Import Confirmation Dialog
    if (showImportConfirmDialog && pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportJson = null
            },
            title = { Text("Uvezi podatke?") },
            text = {
                Text(
                    text = "Ovo će zameniti postojeće podatke sadržajem fajla. Nastaviti?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val json = pendingImportJson!!
                        showImportConfirmDialog = false
                        pendingImportJson = null
                        // Sve-ili-ništa; rezultat (ili greška) stiže kao dijalog.
                        coordinator.importFromJson(json)
                    }
                ) {
                    Text("Uvezi", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        pendingImportJson = null
                    }
                ) {
                    Text("Odustani")
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = { Text("Odjavi se?") },
            text = {
                Text(
                    text = "Podaci ostaju sačuvani lokalno. Možeš se ponovo prijaviti bilo kada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmDialog = false
                        scope.launch {
                            backup.signOut()
                        }
                    }
                ) {
                    Text("Odjavi se", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Odustani")
                }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: ExerciseCategory, isCompound: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.GRUDI) }
    var isCompound by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj vežbu") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naziv vežbe", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text("npr. Cable Fly", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategorija", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ExerciseCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(categoryColor(category), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.displayName)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Compound/Isolation toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCompound) "Složena vežba" else "Izolaciona vežba",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCompound) "Više mišićnih grupa" else "Jedna mišićna grupa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCompound,
                        onCheckedChange = { isCompound = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = UiTokens.Compound,
                            checkedTrackColor = UiTokens.Compound.copy(alpha = 0.5f),
                            uncheckedThumbColor = UiTokens.Isolation,
                            uncheckedTrackColor = UiTokens.Isolation.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedCategory, isCompound) },
                enabled = name.isNotBlank()
            ) {
                Text("Dodaj", color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}
