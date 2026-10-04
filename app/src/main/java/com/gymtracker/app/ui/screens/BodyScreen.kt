package com.gymtracker.app.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.app.data.model.ActivityLevel
import com.gymtracker.app.data.model.CalorieCalculator
import com.gymtracker.app.data.model.Gender
import com.gymtracker.app.data.model.WeightEntry
import com.gymtracker.app.data.repository.BodyMetricsRepository
import com.gymtracker.app.ui.theme.CardShape
import com.gymtracker.app.ui.theme.UiTokens
import com.gymtracker.app.data.model.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyContent(
    bodyMetricsRepository: BodyMetricsRepository,
    modifier: Modifier = Modifier
) {
    val state by bodyMetricsRepository.state.collectAsState()
    var showAddWeightDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WeightEntry?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Telo"
                    )
                },
                actions = {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Izmeni profil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current weight card
                item {
                    CurrentWeightCard(
                        currentWeight = state.currentWeight,
                        goalWeight = state.profile.goalWeightKg,
                        totalChange = state.totalWeightChange,
                        isGaining = state.isGainingWeight()
                    )
                }

                // Calorie info card
                item {
                    CalorieInfoCard(
                        tdee = state.tdee,
                        targetCalories = state.targetCaloriesForGrowth,
                        proteinTarget = state.proteinTarget,
                        bmr = state.bmr
                    )
                }

                // Macro breakdown card
                if (state.targetCaloriesForGrowth != null) {
                    item {
                        MacroBreakdownCard(targetCalories = state.targetCaloriesForGrowth!!)
                    }
                }

                // Weight history
                if (state.weightEntries.isNotEmpty()) {
                    item {
                        Text(
                            text = "ISTORIJA TEŽINE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    items(state.weightHistory.reversed().take(10), key = { it.id }) { entry ->
                        WeightEntryCard(
                            entry = entry,
                            onDelete = { entryToDelete = entry }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // FAB to add weight
        FloatingActionButton(
            onClick = { showAddWeightDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Dodaj težinu"
            )
        }
    }

    // Add weight dialog
    if (showAddWeightDialog) {
        AddWeightDialog(
            onDismiss = { showAddWeightDialog = false },
            onConfirm = { weight ->
                bodyMetricsRepository.addWeightEntry(weight)
                showAddWeightDialog = false
            }
        )
    }

    // Edit profile dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentProfile = state.profile,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { height, birthYear, gender, activity, goalWeight ->
                bodyMetricsRepository.updateProfile(
                    heightCm = height,
                    birthYear = birthYear,
                    gender = gender,
                    activityLevel = activity,
                    goalWeightKg = goalWeight
                )
                showEditProfileDialog = false
            }
        )
    }

    // Delete confirmation dialog
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Obriši unos?") },
            text = { Text("Da li želiš da obrišeš unos od ${entry.weightKg}kg?") },
            confirmButton = {
                TextButton(onClick = {
                    bodyMetricsRepository.deleteWeightEntry(entry.id)
                    entryToDelete = null
                }) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Odustani")
                }
            }
        )
    }
}

@Composable
private fun CurrentWeightCard(
    currentWeight: Float?,
    goalWeight: Float?,
    totalChange: Float?,
    isGaining: Boolean?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MonitorWeight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentWeight != null) {
                Text(
                    text = "%.1f kg".format(currentWeight),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (totalChange != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (totalChange >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (totalChange >= 0) UiTokens.Success else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%+.1f kg ukupno".format(totalChange),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (totalChange >= 0) UiTokens.Success else MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (goalWeight != null) {
                    Text(
                        text = "Cilj: %.1f kg".format(goalWeight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "Dodaj svoju težinu",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Klikni + dugme ispod",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalorieInfoCard(
    tdee: Int?,
    targetCalories: Int?,
    proteinTarget: Int?,
    bmr: Int?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = UiTokens.Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Kalorije",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tdee != null && targetCalories != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalorieStatItem(
                        label = "Održavanje",
                        value = "$tdee kcal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CalorieStatItem(
                        label = "Za rast",
                        value = "$targetCalories kcal",
                        color = UiTokens.Success
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalorieStatItem(
                        label = "BMR",
                        value = "${bmr ?: 0} kcal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CalorieStatItem(
                        label = "Protein cilj",
                        value = "${proteinTarget ?: 0}g",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Jedi $targetCalories kcal dnevno za rast mišića (+400 iznad maintenance)",
                    style = MaterialTheme.typography.bodySmall,
                    color = UiTokens.Success
                )
            } else {
                Text(
                    text = "Dodaj težinu i popuni profil da vidiš kalorijske potrebe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalorieStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroBreakdownCard(targetCalories: Int) {
    val (carbs, protein, fat) = CalorieCalculator.calculateMacros(targetCalories)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Makro raspored (60/25/15)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem(
                    name = "Ugljeni",
                    grams = carbs,
                    color = UiTokens.Warning
                )
                MacroItem(
                    name = "Proteini",
                    grams = protein,
                    color = MaterialTheme.colorScheme.error
                )
                MacroItem(
                    name = "Masti",
                    grams = fat,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun MacroItem(
    name: String,
    grams: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${grams}g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeightEntryCard(
    entry: WeightEntry,
    onDelete: () -> Unit
) {
    val displayDate = entry.dateAsLocalDate?.format(Config.displayFormatter) ?: entry.date

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "%.1f kg".format(entry.weightKg),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Obriši",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeightDialog(
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var weightText by remember { mutableStateOf("") }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj težinu") },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Težina (kg)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                placeholder = { Text("npr. 75.5", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    weightText.toFloatOrNull()?.let { onConfirm(it) }
                },
                enabled = weightText.toFloatOrNull() != null
            ) {
                Text("Dodaj", color = if (weightText.toFloatOrNull() != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    currentProfile: com.gymtracker.app.data.model.NutritionProfile,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Gender, ActivityLevel, Float?) -> Unit
) {
    var heightText by remember { mutableStateOf(currentProfile.heightCm.toString()) }
    var birthYearText by remember { mutableStateOf(currentProfile.birthYear.toString()) }
    var gender by remember { mutableStateOf(currentProfile.gender) }
    var activityLevel by remember { mutableStateOf(currentProfile.activityLevel) }
    var goalWeightText by remember { mutableStateOf(currentProfile.goalWeightKg?.toString() ?: "") }

    var genderExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Izmeni profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { c -> c.isDigit() } },
                    label = { Text("Visina (cm)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = birthYearText,
                    onValueChange = { birthYearText = it.filter { c -> c.isDigit() } },
                    label = { Text("Godina rođenja", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it }
                ) {
                    OutlinedTextField(
                        value = gender.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pol", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        Gender.entries.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.displayName) },
                                onClick = {
                                    gender = g
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                // Activity level dropdown
                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = it }
                ) {
                    OutlinedTextField(
                        value = activityLevel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Nivo aktivnosti", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false }
                    ) {
                        ActivityLevel.entries.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level.displayName) },
                                onClick = {
                                    activityLevel = level
                                    activityExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = goalWeightText,
                    onValueChange = { goalWeightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Ciljna težina (kg) - opciono", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val height = heightText.toIntOrNull() ?: currentProfile.heightCm
                    val birthYear = birthYearText.toIntOrNull() ?: currentProfile.birthYear
                    val goalWeight = goalWeightText.toFloatOrNull()
                    onConfirm(height, birthYear, gender, activityLevel, goalWeight)
                }
            ) {
                Text("Sačuvaj", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}
