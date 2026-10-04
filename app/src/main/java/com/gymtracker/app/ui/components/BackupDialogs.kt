package com.gymtracker.app.ui.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.gymtracker.app.data.backup.BackupCoordinator
import com.gymtracker.app.data.backup.BackupPrompt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val copyDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun formatCopyTime(millis: Long): String =
    if (millis <= 0L) "nepoznat datum"
    else Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(copyDateFormat)

/**
 * Dijalozi backup-a/vraćanja. Stoje u MainScreen-u (iznad tabova), pa se pitanja i
 * rezultat vide bez obzira na kom je tabu korisnik kad se posao završi.
 */
@Composable
fun BackupDialogs(coordinator: BackupCoordinator) {
    val prompt by coordinator.prompt.collectAsState()
    val message by coordinator.message.collectAsState()
    val copies by coordinator.copies.collectAsState()

    when (val current = prompt) {
        BackupPrompt.None -> {}

        BackupPrompt.OfferRestoreAfterSignIn -> AlertDialog(
            onDismissRequest = { coordinator.dismissPrompt() },
            title = { Text("Nađen backup u oblaku") },
            text = {
                Text(
                    "Na ovom Google nalogu već postoji backup. Hoćeš li da vratiš te podatke? " +
                        "Trenutni podaci na telefonu biće zamenjeni."
                )
            },
            confirmButton = {
                PrimaryDialogButton("Vrati iz oblaka") { coordinator.restore() }
            },
            dismissButton = {
                TextButton(onClick = { coordinator.dismissPrompt() }) { Text("Ne sada", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )

        is BackupPrompt.CloudNotAdopted -> AlertDialog(
            onDismissRequest = { coordinator.dismissPrompt() },
            title = { Text("U oblaku već postoji backup") },
            text = {
                Text(
                    "Ova instalacija još nije preuzela podatke iz oblaka. Ako sada napraviš backup, " +
                        "backup u oblaku biće zamenjen podacima sa ovog telefona" +
                        (if (current.localIsEmpty) ", a na telefonu trenutno NEMA podataka" else "") +
                        ". Preporuka: prvo vrati podatke iz oblaka."
                )
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryDialogButton("Vrati iz oblaka") { coordinator.restore() }
                    TextButton(onClick = { coordinator.backupAnyway() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ipak napravi backup", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { coordinator.dismissPrompt() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Otkaži", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )

        BackupPrompt.EmptyLocalOverCloud -> AlertDialog(
            onDismissRequest = { coordinator.dismissPrompt() },
            title = { Text("Na telefonu nema podataka") },
            text = {
                Text(
                    "U oblaku postoji backup, a na telefonu trenutno nema nijednog zapisa. " +
                        "Ako nastaviš, backup u oblaku biće zamenjen praznim."
                )
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryDialogButton("Vrati iz oblaka") { coordinator.restore() }
                    TextButton(onClick = { coordinator.backup(confirmed = true) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ipak napravi backup", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { coordinator.dismissPrompt() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Otkaži", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )

        is BackupPrompt.EmptyBackupOverLocal -> AlertDialog(
            onDismissRequest = { coordinator.dismissPrompt() },
            title = { Text("Backup je prazan") },
            text = {
                Text(
                    "Backup ne sadrži nijedan zapis, a na telefonu ima zapisa: ${current.localCount}. " +
                        "Ako nastaviš, lokalni podaci biće obrisani."
                )
            },
            confirmButton = {
                TextButton(onClick = { coordinator.confirmPendingRestore() }) {
                    Text("Ipak vrati", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { coordinator.dismissPrompt() }) { Text("Otkaži", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    copies?.let { list ->
        AlertDialog(
            onDismissRequest = { coordinator.dismissCopies() },
            title = { Text("Kopije u oblaku") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Izaberi kopiju koju vraćaš. Trenutni podaci na telefonu biće zamenjeni.")
                    list.forEach { copy ->
                        TextButton(onClick = { coordinator.restore(copy) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                formatCopyTime(copy.createdAtMillis) + if (copy.isCurrent) " (trenutna)" else "",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { coordinator.dismissCopies() }) { Text("Otkaži", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }

    message?.let { current ->
        AlertDialog(
            onDismissRequest = { coordinator.dismissMessage() },
            title = { Text(current.title) },
            text = { Text(current.text) },
            confirmButton = {
                TextButton(onClick = { coordinator.dismissMessage() }) {
                    Text("U redu", color = if (current.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun PrimaryDialogButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}
