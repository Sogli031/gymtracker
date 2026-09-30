package com.gymtracker.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymtracker.app.data.model.CustomExercise
import com.gymtracker.app.data.model.Exercise
import com.gymtracker.app.data.model.ExerciseCategory
import com.gymtracker.app.ui.theme.CardShape
import com.gymtracker.app.ui.theme.UiTokens
import com.gymtracker.app.ui.theme.categoryColor

/**
 * Sadržaj bottom sheet-a za biranje vežbe.
 * Podržava predefinisane i custom vežbe grupisane po kategorijama.
 */
@Composable
fun ExercisePickerContent(
    customExercises: List<CustomExercise>,
    onExerciseSelected: (Exercise) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Text(
            text = "Izaberi vežbu",
            style = MaterialTheme.typography.headlineSmall,
            color = UiTokens.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ExerciseCategory.entries.toList()) { category ->
                val builtInExercises = Exercise.byCategory(category)
                val customForCategory = customExercises
                    .filter { it.category == category }
                    .map { it.toExercise() }
                val allExercises = (builtInExercises + customForCategory)
                    .sortedByDescending { it.isCompound }

                CategoryPickerSection(
                    category = category,
                    exercises = allExercises,
                    customExerciseNames = customForCategory.map { it.name }.toSet(),
                    onExerciseClick = onExerciseSelected
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CategoryPickerSection(
    category: ExerciseCategory,
    exercises: List<Exercise>,
    customExerciseNames: Set<String> = emptySet(),
    onExerciseClick: (Exercise) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val color = categoryColor(category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        shape = CardShape
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = UiTokens.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${exercises.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UiTokens.TextMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Skupi" else "Proširi",
                    tint = UiTokens.TextMuted
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    exercises.forEach { exercise ->
                        val isCustom = exercise.name in customExerciseNames
                        val exerciseColor = if (isCustom) UiTokens.Accent
                            else if (exercise.isCompound) UiTokens.Compound
                            else UiTokens.Isolation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExerciseClick(exercise) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(exerciseColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = exerciseColor
                            )
                            if (isCustom) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(moja)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UiTokens.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
