package com.gymtracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
