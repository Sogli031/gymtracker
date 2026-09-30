package com.gymtracker.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymtracker.app.data.model.ExerciseSet
import com.gymtracker.app.ui.theme.UiTokens

@Composable
fun SetRow(
    setNumber: Int,
    set: ExerciseSet,
    placeholderKg: Double?,
    placeholderReps: Int?,
    onUpdate: (Double?, Int?) -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var kgText by remember(set.id) {
        mutableStateOf(set.kg?.toString() ?: "")
    }
    var repsText by remember(set.id) {
        mutableStateOf(set.reps?.toString() ?: "")
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$setNumber.",
            color = UiTokens.TextSecondary,
            modifier = Modifier.width(32.dp)
        )

        OutlinedTextField(
            value = kgText,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                    .replace(',', '.')
                kgText = filtered
                onUpdate(filtered.toDoubleOrNull(), repsText.toIntOrNull())
            },
            placeholder = {
                Text(
                    text = placeholderKg?.toString() ?: "kg",
                    color = UiTokens.TextMuted.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = repsText,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                repsText = filtered
                onUpdate(kgText.toDoubleOrNull(), filtered.toIntOrNull())
            },
            placeholder = {
                Text(
                    text = placeholderReps?.toString() ?: "reps",
                    color = UiTokens.TextMuted.copy(alpha = 0.5f)
                )
            },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = textFieldColors
        )

        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Obriši set",
                    tint = UiTokens.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
