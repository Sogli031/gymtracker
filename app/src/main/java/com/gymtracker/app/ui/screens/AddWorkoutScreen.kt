package com.gymtracker.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gymtracker.app.data.model.Config
import com.gymtracker.app.data.model.ExerciseCategory
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.data.model.WorkoutExercise
import com.gymtracker.app.data.repository.CustomExerciseRepository
import com.gymtracker.app.data.repository.MachineSettingsRepository
import com.gymtracker.app.data.repository.WorkoutRepository
import com.gymtracker.app.data.model.MachineSettings
import com.gymtracker.app.ui.components.ExerciseCard
import com.gymtracker.app.ui.components.MachineSettingsDialog
import com.gymtracker.app.ui.theme.AppGradient
import com.gymtracker.app.ui.theme.UiTokens
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutScreen(
    navController: NavController,
    repository: WorkoutRepository,
    customExerciseRepository: CustomExerciseRepository? = null,
    machineSettingsRepository: MachineSettingsRepository? = null,
    workoutId: String? = null,
    copyFromWorkoutId: String? = null
) {
    val customExercises by (customExerciseRepository?.exercises
        ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val isEditing = workoutId != null
    val isRepoLoaded by repository.isLoaded.collectAsState()
    val existingWorkout = workoutId?.let { repository.get(it) }
    val copyFromWorkout = copyFromWorkoutId?.let { repository.get(it) }

    // Ako uređujemo ili kopiramo trening, a repozitorijum još nije učitan iz baze
    // (npr. posle process-death dok se Room ponovo ne popuni), repository.get()
    // vraća null. Bez ovog čekanja bi ekran greškom seedovao default šablon i pri
    // snimanju napravio NOV trening umesto da izmeni postojeći. Sačekaj učitavanje.
    val awaitingWorkout = (workoutId != null && existingWorkout == null) ||
        (copyFromWorkoutId != null && copyFromWorkout == null)
    if (awaitingWorkout && !isRepoLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradient),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = UiTokens.Accent)
        }
        return
    }

    var nameText by rememberSaveable {
        mutableStateOf(
            existingWorkout?.name
                ?: copyFromWorkout?.name
                ?: ""
        )
    }
    var dateText by rememberSaveable {
        mutableStateOf(
            existingWorkout?.displayDate
                ?: LocalDate.now().format(Config.displayFormatter)
        )
    }
    var timeText by rememberSaveable {
        mutableStateOf(
            existingWorkout?.displayTime
                ?: LocalTime.now().format(Config.timeFormatter)
        )
    }
    var savedDraftId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasNavigatedAway by remember { mutableStateOf(false) }

    // Podrazumevani šablon za novi trening (celo telo)
    fun templateExercise(name: String, category: ExerciseCategory, sets: Int) =
        WorkoutExercise(
            exerciseName = name,
            category = category,
            sets = List(sets) { com.gymtracker.app.data.model.ExerciseSet() }
        )

    // Leđa se smenjuju: ako je poslednji odrađen trening imao Lat Pulldown, sada ide Cable Row, i obrnuto.
    val backExercise = if (existingWorkout == null && copyFromWorkout == null) {
        val lastBack = repository.workouts.value
            .sortedWith(compareByDescending<Workout> { it.date }.thenByDescending { it.time })
            .asSequence()
            .flatMap { it.exercises.asSequence() }
            .firstOrNull { ex ->
                (ex.exerciseName.equals("Lat Pulldown", ignoreCase = true) ||
                    ex.exerciseName.equals("Cable Row", ignoreCase = true)) &&
                    ex.sets.any { it.isComplete }
            }
        if (lastBack?.exerciseName.equals("Lat Pulldown", ignoreCase = true)) "Cable Row" else "Lat Pulldown"
    } else "Lat Pulldown"

    // Grudi se smenjuju: ako je poslednji trening imao Chest Press Machine, sada ide Bench Press, i obrnuto.
    val chestExercise = if (existingWorkout == null && copyFromWorkout == null) {
        val lastChest = repository.workouts.value
            .sortedWith(compareByDescending<Workout> { it.date }.thenByDescending { it.time })
            .asSequence()
            .flatMap { it.exercises.asSequence() }
            .firstOrNull { ex ->
                (ex.exerciseName.equals("Chest Press Machine", ignoreCase = true) ||
                    ex.exerciseName.equals("Bench Press", ignoreCase = true)) &&
                    ex.sets.any { it.isComplete }
            }
        if (lastChest?.exerciseName.equals("Chest Press Machine", ignoreCase = true)) "Bench Press" else "Chest Press Machine"
    } else "Chest Press Machine"

    val defaultTemplate = listOf(
        templateExercise(chestExercise, ExerciseCategory.GRUDI, 3),
        templateExercise(backExercise, ExerciseCategory.LEDJA, 3),
        templateExercise("Leg Press", ExerciseCategory.NOGE, 3),
        templateExercise("Cable Overhead Extension", ExerciseCategory.TRICEPS, 2),
        templateExercise("Dumbbell Curl", ExerciseCategory.BICEPS, 2),
        templateExercise("Lateral Raise", ExerciseCategory.RAMENA, 2)
    )

    // Initialize exercises from existing or copied workout, or use default template
    var exercises by remember {
        mutableStateOf(
            existingWorkout?.exercises
                ?: copyFromWorkout?.exercises?.map { it.copyFresh() }
                ?: defaultTemplate
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var dateError by remember { mutableStateOf<String?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var exerciseToReplace by remember { mutableStateOf<WorkoutExercise?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<WorkoutExercise?>(null) }
    var exerciseForMachineSettings by remember { mutableStateOf<WorkoutExercise?>(null) }
    // Štoperica: kreće kad se upišu ponavljanja serije, staje na poslednjoj seriji poslednje vežbe.
    var timerStartMs by remember { mutableStateOf<Long?>(null) }
    var timerSetId by remember { mutableStateOf<String?>(null) }
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) timerStartMs?.let { com.gymtracker.app.util.RestNotifier.show(appContext, it) }
    }
    // Obaveštenje se pojavljuje odmah čim štoperica krene (i ostaje dok traje), nestaje kad stane.
    LaunchedEffect(timerStartMs) {
        val start = timerStartMs
        if (start == null) {
            com.gymtracker.app.util.RestNotifier.cancel(appContext)
        } else {
            com.gymtracker.app.util.RestNotifier.show(appContext, start)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { com.gymtracker.app.util.RestNotifier.cancel(appContext) }
    }

    fun saveAndExit() {
        if (dateError != null) return
        hasNavigatedAway = true
        if (exercises.isNotEmpty()) {
            saveWorkout(
                name = nameText.ifBlank { null },
                dateText = dateText,
                timeText = timeText,
                exercises = exercises,
                existingWorkout = existingWorkout,
                savedDraftId = savedDraftId,
                repository = repository,
                navController = navController,
                navigateBack = true,
                onWorkoutSaved = { id -> savedDraftId = id }
            )
        } else {
            navController.popBackStack()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !hasNavigatedAway) {
                val hasAnyData = exercises.any { ex ->
                    ex.sets.any { it.kg != null || it.reps != null }
                }
                if (hasAnyData && dateError == null) {
                    val date = try {
                        LocalDate.parse(dateText, Config.displayFormatter)
                    } catch (e: Exception) { null }
                    val time = try {
                        LocalTime.parse(timeText, Config.timeFormatter)
                    } catch (e: Exception) { LocalTime.now() }
                    if (date != null) {
                        val name = nameText.ifBlank { null }
                        val idToUpdate = existingWorkout?.id ?: savedDraftId
                        if (idToUpdate != null) {
                            // Upsert po poznatom id-u; sačuvaj endTime/createdAt iz baze.
                            val base = existingWorkout ?: repository.get(idToUpdate)
                            val workout = base?.copy(
                                name = name, date = date, time = time, exercises = exercises
                            ) ?: Workout(
                                id = idToUpdate, name = name,
                                date = date, time = time, exercises = exercises
                            )
                            runBlocking(Dispatchers.IO) { repository.updateSync(workout) }
                        } else {
                            val w = Workout(
                                name = name, date = date,
                                time = time, exercises = exercises
                            )
                            // Postavi id PRE inserta da ponovni poziv ne napravi duplikat.
                            savedDraftId = w.id
                            runBlocking(Dispatchers.IO) { repository.addSync(w) }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        saveAndExit()
    }

    // Auto-save: čuva u bazu čim se nešto promeni (debounce 1 sekunda)
    LaunchedEffect(exercises, nameText, dateText, timeText) {
        if (hasNavigatedAway) return@LaunchedEffect
        val hasAnyData = exercises.any { ex ->
            ex.sets.any { it.kg != null || it.reps != null }
        }
        if (!hasAnyData || dateError != null) return@LaunchedEffect

        delay(1000L)

        val date = try {
            LocalDate.parse(dateText, Config.displayFormatter)
        } catch (e: Exception) { return@LaunchedEffect }
        val time = try {
            LocalTime.parse(timeText, Config.timeFormatter)
        } catch (e: Exception) { LocalTime.now() }

        val name = nameText.ifBlank { null }
        val idToUpdate = existingWorkout?.id ?: savedDraftId

        if (idToUpdate != null) {
            // Upsert po poznatom id-u (updateSync radi delete+insert, pa je bezbedno
            // i ako red još ne postoji). Sačuvaj endTime/createdAt iz osnovnog treninga.
            val base = existingWorkout ?: repository.get(idToUpdate)
            val workout = base?.copy(
                name = name, date = date, time = time, exercises = exercises
            ) ?: Workout(
                id = idToUpdate, name = name,
                date = date, time = time, exercises = exercises
            )
            withContext(Dispatchers.IO) {
                repository.updateSync(workout)
            }
        } else {
            val newWorkout = Workout(
                name = name, date = date,
                time = time, exercises = exercises
            )
            // Postavi id PRE inserta: ako se ovaj effect prekine (dalji unos) posle
            // inserta, sledeći ciklus vidi id i radi upsert umesto novog reda (bez duplikata).
            savedDraftId = newWorkout.id
            withContext(Dispatchers.IO) {
                repository.addSync(newWorkout)
            }
        }
    }

    val placeholders = remember(exercises, savedDraftId) {
        exercises.associate { exercise ->
            exercise.exerciseName to repository.getLastSetsForExercise(
                exercise.exerciseName,
                excludeWorkoutId = existingWorkout?.id ?: savedDraftId
            )
        }
    }

    val allMachineSettings by (machineSettingsRepository?.settings
        ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    val machineSettingsMap = remember(allMachineSettings, exercises) {
        exercises.associate { exercise ->
            exercise.exerciseName to (allMachineSettings.firstOrNull {
                it.exerciseName.equals(exercise.exerciseName, ignoreCase = true)
            }?.settings ?: emptyList())
        }
    }

    val previousBest1RM = remember(exercises, existingWorkout) {
        if (existingWorkout != null) {
            // Editovanje: poredi sa treningom PRE ovog (hronološki stariji)
            exercises.associate { exercise ->
                val sets = repository.getSetsForExerciseBefore(
                    exercise.exerciseName,
                    beforeWorkoutId = existingWorkout.id
                )
                exercise.exerciseName to sets.mapNotNull { it.estimated1RM }.maxOrNull()
            }
        } else {
            // Novi trening: poredi sa poslednjim
            placeholders.mapValues { (_, sets) ->
                sets.mapNotNull { it.estimated1RM }.maxOrNull()
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isEditing -> "Izmeni trening"
                            else -> "Novi trening"
                        },
                        color = UiTokens.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveAndExit()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Nazad",
                            tint = UiTokens.TextPrimary
                        )
                    }
                },
                actions = {
                    if (isEditing && existingWorkout != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Više opcija",
                                    tint = UiTokens.TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ponovi isti trening") },
                                    onClick = {
                                        showMenu = false
                                        navController.popBackStack()
                                        navController.navigate("repeat_workout/${existingWorkout.id}")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Obriši", color = UiTokens.Danger) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = UiTokens.Danger
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppGradient)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Naziv treninga (opciono)", color = UiTokens.TextMuted) },
                        placeholder = { Text("npr. Push Day, Legs...", color = UiTokens.TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors
                    )
                }

                // Date and time row
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = {
                                    dateText = it
                                    dateError = try {
                                        LocalDate.parse(it, Config.displayFormatter)
                                        null
                                    } catch (e: Exception) {
                                        if (it.isNotBlank()) "Neispravan format datuma (dd.MM.yyyy)" else null
                                    }
                                },
                                label = { Text("Datum", color = UiTokens.TextMuted) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                isError = dateError != null,
                                colors = textFieldColors
                            )
                            OutlinedTextField(
                                value = timeText,
                                onValueChange = { timeText = it },
                                label = { Text("Vreme", color = UiTokens.TextMuted) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = textFieldColors
                            )
                        }
                        dateError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Exercises
                items(
                    items = exercises,
                    key = { it.id }
                ) { exercise ->
                    val exerciseIndex = exercises.indexOf(exercise)
                    Column {
                        ExerciseCard(
                            exercise = exercise,
                            placeholderSets = placeholders[exercise.exerciseName] ?: emptyList(),
                            targetOneRM = previousBest1RM[exercise.exerciseName],
                            machineSettings = machineSettingsMap[exercise.exerciseName] ?: emptyList(),
                            onEditMachineSettings = {
                                exerciseForMachineSettings = exercise
                            },
                            timerSetId = timerSetId,
                            timerStartMs = timerStartMs,
                            onSetUpdate = { setId, kg, reps ->
                                val oldReps = exercise.sets.firstOrNull { it.id == setId }?.reps
                                if (reps != null && reps > 0 && reps != oldReps) {
                                    if (exerciseIndex == exercises.size - 1 &&
                                        exercise.sets.lastOrNull()?.id == setId
                                    ) {
                                        timerStartMs = null
                                        timerSetId = null
                                    } else {
                                        timerStartMs = System.currentTimeMillis()
                                        timerSetId = setId
                                    }
                                }
                                exercises = exercises.map {
                                    if (it.id == exercise.id) {
                                        it.updateSet(setId, kg, reps)
                                    } else it
                                }
                            },
                            onAddSet = {
                                exercises = exercises.map {
                                    if (it.id == exercise.id) it.addSet() else it
                                }
                            },
                            onRemoveSet = { setId ->
                                exercises = exercises.map {
                                    if (it.id == exercise.id) it.removeSet(setId) else it
                                }
                            },
                            onRemoveExercise = {
                                exerciseToDelete = exercise
                            },
                            onReplaceExercise = {
                                exerciseToReplace = exercise
                                showExercisePicker = true
                            },
                            onMoveUp = if (exerciseIndex > 0) {
                                {
                                    val newList = exercises.toMutableList()
                                    val item = newList.removeAt(exerciseIndex)
                                    newList.add(exerciseIndex - 1, item)
                                    exercises = newList
                                }
                            } else null,
                            onMoveDown = if (exerciseIndex < exercises.size - 1) {
                                {
                                    val newList = exercises.toMutableList()
                                    val item = newList.removeAt(exerciseIndex)
                                    newList.add(exerciseIndex + 1, item)
                                    exercises = newList
                                }
                            } else null
                        )
                    }
                }

                item {
                    TextButton(
                        onClick = {
                            exerciseToReplace = null
                            showExercisePicker = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = UiTokens.AccentAlt
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Dodaj vežbu",
                            color = UiTokens.AccentAlt,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Exercise picker bottom sheet
    if (showExercisePicker) {
        ModalBottomSheet(
            onDismissRequest = {
                showExercisePicker = false
                exerciseToReplace = null
            },
            sheetState = sheetState,
            containerColor = UiTokens.BgTop
        ) {
            ExercisePickerContent(
                customExercises = customExercises,
                onExerciseSelected = { selectedExercise ->
                    val replacing = exerciseToReplace
                    if (replacing != null) {
                        exercises = exercises.map { exercise ->
                            if (exercise.id == replacing.id) {
                                exercise.copy(
                                    exerciseName = selectedExercise.name,
                                    category = selectedExercise.category
                                )
                            } else {
                                exercise
                            }
                        }
                    } else {
                        val newWorkoutExercise = WorkoutExercise(
                            exerciseName = selectedExercise.name,
                            category = selectedExercise.category
                        )
                        exercises = exercises + newWorkoutExercise
                    }
                    scope.launch {
                        sheetState.hide()
                        showExercisePicker = false
                        exerciseToReplace = null
                    }
                }
            )
        }
    }
    // Delete workout dialog
    if (showDeleteDialog && existingWorkout != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Obriši trening?") },
            text = {
                Column {
                    Text("Da li si siguran da želiš da obrišeš ovaj trening?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.delete(existingWorkout.id)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Obriši", color = UiTokens.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Odustani")
                }
            },
            containerColor = UiTokens.Surface,
            titleContentColor = UiTokens.TextPrimary,
            textContentColor = UiTokens.TextSecondary
        )
    }

    // Delete exercise dialog
    exerciseToDelete?.let { exerciseToRemove ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            title = { Text("Obriši vežbu?") },
            text = {
                Text("Da li želiš da obrišeš \"${exerciseToRemove.exerciseName}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        exercises = exercises.filter { it.id != exerciseToRemove.id }
                        exerciseToDelete = null
                    }
                ) {
                    Text("Obriši", color = UiTokens.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Odustani")
                }
            },
            containerColor = UiTokens.Surface,
            titleContentColor = UiTokens.TextPrimary,
            textContentColor = UiTokens.TextSecondary
        )
    }

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
}

private fun saveWorkout(
    name: String?,
    dateText: String,
    timeText: String,
    exercises: List<WorkoutExercise>,
    existingWorkout: Workout?,
    savedDraftId: String?,
    repository: WorkoutRepository,
    navController: NavController,
    navigateBack: Boolean = true,
    onWorkoutSaved: ((String) -> Unit)? = null
) {
    // Čuvaj čim postoji bilo kakav unet podatak (kg ili reps)
    val hasAnyData = exercises.any { exercise ->
        exercise.sets.any { it.kg != null || it.reps != null }
    }
    if (!hasAnyData) {
        if (navigateBack) navController.popBackStack()
        return
    }

    val date = try {
        LocalDate.parse(dateText, Config.displayFormatter)
    } catch (e: Exception) {
        if (navigateBack) navController.popBackStack()
        return
    }

    val time = try {
        LocalTime.parse(timeText, Config.timeFormatter)
    } catch (e: Exception) {
        LocalTime.now()
    }

    if (existingWorkout != null) {
        repository.update(
            existingWorkout.copy(
                name = name,
                date = date,
                time = time,
                exercises = exercises
            )
        )
        onWorkoutSaved?.invoke(existingWorkout.id)
    } else if (savedDraftId != null) {
        val draft = repository.get(savedDraftId)
        if (draft != null) {
            repository.update(
                draft.copy(
                    name = name,
                    date = date,
                    time = time,
                    exercises = exercises
                )
            )
            onWorkoutSaved?.invoke(savedDraftId)
        } else {
            // Koristi isti ID da ne bi bilo duplikata
            val newWorkout = Workout(id = savedDraftId!!, name = name, date = date, time = time, exercises = exercises)
            repository.add(newWorkout)
            onWorkoutSaved?.invoke(savedDraftId!!)
        }
    } else {
        val newWorkout = Workout(
            name = name,
            date = date,
            time = time,
            exercises = exercises
        )
        repository.add(newWorkout)
        onWorkoutSaved?.invoke(newWorkout.id)
    }

    if (navigateBack) navController.popBackStack()
}
