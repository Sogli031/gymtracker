package com.gymtracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymtracker.app.data.model.Workout
import com.gymtracker.app.ui.theme.CardShape
import com.gymtracker.app.ui.theme.UiTokens
import com.gymtracker.app.ui.theme.categoryColor

@Composable
fun WorkoutCard(
    workout: Workout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = UiTokens.Surface),
        shape = CardShape
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Name and date row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.displayName,
                        color = UiTokens.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${workout.displayDate} · ${workout.displayTime}",
                        color = UiTokens.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category indicators
            if (workout.exercises.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    workout.exercises.map { it.category }.distinct().take(5).forEach { category ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(categoryColor(category), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = workout.categorySummary,
                        color = UiTokens.Accent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "${workout.totalExercises} vežbi · ${workout.totalSets} serija",
                color = UiTokens.TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
