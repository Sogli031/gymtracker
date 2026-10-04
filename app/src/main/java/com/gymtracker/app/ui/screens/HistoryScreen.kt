package com.gymtracker.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.data.model.WorkoutTemplate
import com.gymtracker.app.data.repository.WorkoutRepository
import com.gymtracker.app.ui.components.WorkoutCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogContent(
    navController: NavController,
    repository: WorkoutRepository,
    modifier: Modifier = Modifier
) {
    val workouts by repository.workouts.collectAsState()
    val isLoaded by repository.isLoaded.collectAsState()
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Log",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )

            if (!isLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Učitavanje...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (workouts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nema treninga",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pritisni „Trening“ da dodaš prvi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = workouts,
                        key = { it.id }
                    ) { workout ->
                        SwipeableWorkoutCard(
                            workout = workout,
                            onDelete = { workoutToDelete = workout },
                            onClick = {
                                navController.navigate("edit_workout/${workout.id}")
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showTemplatePicker = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Trening") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    // „+“: izbor vrste treninga (Volume = dosadašnji šablon, HIT = sve po 1 seriju)
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Novi trening") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutTemplate.entries.forEach { template ->
                        TemplateOption(
                            template = template,
                            onClick = {
                                showTemplatePicker = false
                                navController.navigate("add_workout?template=${template.name}")
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) {
                    Text("Odustani")
                }
            }
        )
    }

    // Delete confirmation dialog
    workoutToDelete?.let { workout ->
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Obriši trening?") },
            text = {
                Column {
                    Text("Da li si siguran da želiš da obrišeš trening od ${workout.displayDate}?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.delete(workout.id)
                        workoutToDelete = null
                    }
                ) {
                    Text("Obriši", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Odustani")
                }
            }
        )
    }
}

@Composable
private fun TemplateOption(template: WorkoutTemplate, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = template.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableWorkoutCard(
    workout: Workout,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Obriši",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        content = {
            WorkoutCard(
                workout = workout,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    )
}
