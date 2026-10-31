package de.pquiel.workout.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pquiel.workout.BackupState
import de.pquiel.workout.model.HistoryFile

private const val PERSIST_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

/** Neue Sicherungsdatei anlegen, mit dauerhaftem Lese- und Schreibzugriff. */
private class CreateBackupFile : ActivityResultContracts.CreateDocument("text/plain") {
    override fun createIntent(context: Context, input: String): Intent =
        super.createIntent(context, input).addFlags(PERSIST_FLAGS)
}

/** Vorhandene Sicherungsdatei öffnen, mit dauerhaftem Lese- und Schreibzugriff. */
private class OpenBackupFile : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).addFlags(PERSIST_FLAGS)
}

private val ErrorRed = Color(0xFFB91C1C)

/** Sicherungsdatei des Verlaufs auf dem Handy, die eine Neuinstallation der App übersteht. */
@Composable
fun BackupCard(
    state: BackupState,
    onCreate: (Uri) -> Unit,
    onOpen: (Uri) -> Unit,
    onUnlink: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val create = rememberLauncherForActivityResult(CreateBackupFile()) { uri -> uri?.let(onCreate) }
    val open = rememberLauncherForActivityResult(OpenBackupFile()) { uri -> uri?.let(onOpen) }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SICHERUNGSDATEI", fontWeight = FontWeight.Black, color = Navy)
            if (state.linked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = NavyLight, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(state.fileName.orEmpty(), fontWeight = FontWeight.Bold, color = Navy)
                }
                Text(
                    "Jedes Training wird zusätzlich in dieser Datei auf dem Handy gespeichert. Sie bleibt beim " +
                        "Deinstallieren der App erhalten – nach einer Neuinstallation hier „Vorhandene Datei laden“ wählen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onUnlink, enabled = !state.busy) { Text("Datei trennen", color = Navy) }
            } else {
                Text(
                    "Speichere den Verlauf zusätzlich in einer Datei auf dem Handy (z. B. im Ordner „Download“ oder " +
                        "„Documents“). Sie bleibt beim Deinstallieren erhalten und kann von einer neuen Version der " +
                        "App wieder eingelesen werden.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = { create.launch(HistoryFile.DEFAULT_NAME) },
                    enabled = !state.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Neue Sicherungsdatei anlegen", fontWeight = FontWeight.Bold) }
            }
            OutlinedButton(
                onClick = { open.launch(arrayOf("text/*", "application/octet-stream")) },
                enabled = !state.busy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.busy) "Bitte warten …" else "Vorhandene Datei laden", color = Navy, fontWeight = FontWeight.Bold)
            }
            state.message?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        message,
                        color = if (state.isError) ErrorRed else NavyLight,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissMessage) { Text("OK", color = Navy) }
                }
            }
        }
    }
}
