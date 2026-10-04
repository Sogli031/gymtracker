package com.gymtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import com.gymtracker.app.data.model.ExerciseSet
import com.gymtracker.app.data.model.MachineSetting
import com.gymtracker.app.data.model.WorkoutExercise
import com.gymtracker.app.ui.theme.CardShape
import com.gymtracker.app.ui.theme.UiTokens
import com.gymtracker.app.ui.theme.categoryColor
import kotlin.math.max
import kotlin.math.round

@Composable
fun ExerciseCard(
    exercise: WorkoutExercise,
    placeholderSets: List<ExerciseSet>,
    targetOneRM: Double?,
    onSetUpdate: (String, Double?, Int?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (String) -> Unit,
    onRemoveExercise: () -> Unit,
    onReplaceExercise: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    machineSettings: List<MachineSetting> = emptyList(),
    onEditMachineSettings: (() -> Unit)? = null,
    timerSetId: String? = null,
    timerStartMs: Long? = null,
    modifier: Modifier = Modifier
) {
    val color = categoryColor(exercise.category)
    // Uzmi poslednji KOMPLETAN set (i kg i reps iz istog seta). Ranije su se kg i reps
    // birali nezavisno (lastOrNull), pa su mogli doći iz dva različita seta i dati
    // pogrešan "Trenutni 1RM".
    // Poredi se NAJBOLJI set (najveći procenjeni 1RM), ne poslednji.
    val lastCompleteSet = exercise.bestSet ?: exercise.sets.lastOrNull { it.isComplete }
    val currentKg = lastCompleteSet?.kg
    val currentReps = lastCompleteSet?.reps
    val targetReps = calculateTargetReps(targetOneRM, currentKg)
    val currentOneRM = calculateBrzycki1RM(currentKg, currentReps)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = exercise.exerciseName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onEditMachineSettings != null) {
                    IconButton(onClick = onEditMachineSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Podešavanja mašine",
                            tint = if (machineSettings.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Ostale akcije u overflow meniju: 48dp touch target bez gušenja naziva vežbe
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opcije vežbe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        if (onReplaceExercise != null) {
                            DropdownMenuItem(
                                text = { Text("Izmeni vežbu") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = { menuOpen = false; onReplaceExercise() }
                            )
                        }
                        if (onMoveUp != null) {
                            DropdownMenuItem(
                                text = { Text("Pomeri gore") },
                                leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) },
                                onClick = { menuOpen = false; onMoveUp() }
                            )
                        }
                        if (onMoveDown != null) {
                            DropdownMenuItem(
                                text = { Text("Pomeri dole") },
                                leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                                onClick = { menuOpen = false; onMoveDown() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Ukloni", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = { menuOpen = false; onRemoveExercise() }
                        )
                    }
                }
            }

            if (machineSettings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    machineSettings.forEach { setting ->
                        Text(
                            text = "${setting.name}: ${setting.value}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Set headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "#",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(32.dp)
                )
                Text(
                    text = "KG",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PONAVLJANJA",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sets
            exercise.sets.forEachIndexed { index, set ->
                val placeholder = placeholderSets.getOrNull(index)
                SetRow(
                    setNumber = index + 1,
                    set = set,
                    placeholderKg = placeholder?.kg,
                    placeholderReps = placeholder?.reps,
                    onUpdate = { kg, reps -> onSetUpdate(set.id, kg, reps) },
                    onRemove = if (exercise.sets.size > 1) {
                        { onRemoveSet(set.id) }
                    } else null
                )

                if (timerStartMs != null && set.id == timerSetId) {
                    RestTimerText(timerStartMs)
                }

                if (index < exercise.sets.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Prikaz progresije ako imamo prethodni 1RM i trenutnu težinu
            if (targetOneRM != null && targetOneRM > 0 && currentKg != null && currentKg > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Prikaz prethodnog 1RM
                    Text(
                        text = "Prethodni 1RM: ${String.format("%.1f", targetOneRM)}kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentReps != null && currentReps > 0) {
                        // Imamo i ponavljanja - možemo izračunati trenutni 1RM
                        val current1RM = currentOneRM ?: 0.0
                        val percent = if (targetOneRM > 0) {
                            ((current1RM - targetOneRM) / targetOneRM) * 100
                        } else 0.0

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append("Trenutni 1RM: ${String.format("%.1f", current1RM)}kg ")
                                append("(")
                                if (percent >= 0) append("+")
                                append(String.format("%.1f", percent))
                                append("%)")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                percent > 0.5 -> UiTokens.Success
                                percent < -0.5 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    } else {
                        // Nemamo ponavljanja - prikaži koliko treba za isti 1RM
                        val repsForSame = targetReps
                        if (repsForSame != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Za isti 1RM uradi $repsForSame ponavljanja",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Show best 1RM for this exercise
            val best1RM = exercise.best1RM
            val hasCompletedSet = exercise.sets.count { it.isComplete } > 0

            if (hasCompletedSet && best1RM != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.1f", best1RM)}kg 1RM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add set button
            TextButton(
                onClick = onAddSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Dodaj seriju",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RestTimerText(startMs: Long) {
    var now by remember(startMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val secs = ((now - startMs) / 1000).coerceAtLeast(0)
    Text(
        text = "⏱ %02d:%02d".format(secs / 60, secs % 60),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 36.dp, top = 4.dp)
    )
}

private fun calculateTargetReps(targetOneRM: Double?, kg: Double?): Int? {
    if (targetOneRM == null || kg == null || targetOneRM <= 0.0 || kg <= 0.0) return null
    val rawReps = (1.0278 - (kg / targetOneRM)) / 0.0278
    if (!rawReps.isFinite()) return null
    return round(max(rawReps, 1.0)).toInt()
}

private fun calculateBrzycki1RM(kg: Double?, reps: Int?): Double? {
    if (kg == null || reps == null || reps <= 0) return null
    if (reps == 1) return kg
    // Brzycki formula nije pouzdana za više od 30 ponavljanja
    if (reps > 30) return null
    val denominator = 1.0278 - 0.0278 * reps
    return if (denominator > 0) kg / denominator else null
}

