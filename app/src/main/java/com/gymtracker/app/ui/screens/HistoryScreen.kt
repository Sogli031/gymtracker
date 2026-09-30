package com.gymtracker.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.gymtracker.app.data.repository.WorkoutRepository
import com.gymtracker.app.ui.components.WorkoutCard
import com.gymtracker.app.ui.theme.UiTokens

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Log",
                        color = UiTokens.TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("add_workout") }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dodaj trening",
                            tint = UiTokens.Accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            if (!isLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Učitavanje...",
                        color = UiTokens.TextMuted
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
                            tint = UiTokens.TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nema treninga",
                            style = MaterialTheme.typography.titleLarge,
                            color = UiTokens.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pritisni + da dodaš prvi trening",
                            color = UiTokens.TextMuted,
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
                    Text("Obriši", color = UiTokens.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Odustani")
                }
            },
            containerColor = UiTokens.Surface,
            titleContentColor = UiTokens.TextPrimary,
            textContentColor = UiTokens.TextSecondary
        )
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
                    SwipeToDismissBoxValue.EndToStart -> UiTokens.Danger.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Obriši",
                        tint = UiTokens.Danger
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
