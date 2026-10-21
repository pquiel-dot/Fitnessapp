package de.pquiel.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.pquiel.workout.strava.StravaState
import de.pquiel.workout.strava.StravaUpload

val StravaOrange = Color(0xFFFC4C02)
private val ErrorRed = Color(0xFFB91C1C)

/** Verbindung zu Strava einrichten bzw. verwalten (Startseite). */
@Composable
fun StravaCard(
    state: StravaState,
    onConnect: (clientId: String, clientSecret: String) -> Unit,
    onDisconnect: () -> Unit,
    onAutoUpload: (Boolean) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Strava", fontWeight = FontWeight.Bold, color = StravaOrange)
            if (state.connected) {
                Text("Verbunden${state.athleteName?.let { " als $it" } ?: ""}")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Nach jedem Training automatisch hochladen", modifier = Modifier.weight(1f))
                    Switch(checked = state.autoUpload, onCheckedChange = onAutoUpload)
                }
                TextButton(onClick = onDisconnect, enabled = !state.busy) {
                    Text("Verbindung trennen", color = Navy)
                }
            } else {
                Credentials(state, onConnect)
            }
            state.message?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissMessage) { Text("OK", color = Navy) }
                }
            }
        }
    }
}

@Composable
private fun Credentials(state: StravaState, onConnect: (String, String) -> Unit) {
    var clientId by rememberSaveable(state.clientId) { mutableStateOf(state.clientId) }
    var clientSecret by rememberSaveable(state.clientSecret) { mutableStateOf(state.clientSecret) }
    Text(
        "Trainings nach jedem Abschluss automatisch als Aktivität zu Strava hochladen. " +
            "Lege dazu einmalig unter strava.com/settings/api eine eigene API-Anwendung an " +
            "(„Autorisierungs-Callback-Domain“: localhost) und trage hier Client-ID und Client-Secret ein.",
        style = MaterialTheme.typography.bodyMedium,
    )
    OutlinedTextField(
        value = clientId,
        onValueChange = { clientId = it.filter(Char::isDigit) },
        label = { Text("Client-ID") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = clientSecret,
        onValueChange = { clientSecret = it.trim() },
        label = { Text("Client-Secret") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { onConnect(clientId, clientSecret) },
        enabled = !state.busy && clientId.isNotBlank() && clientSecret.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.busy) "Verbinde …" else "Mit Strava verbinden", fontWeight = FontWeight.Bold)
    }
}

/** Kurztext zum Upload-Stand eines Trainings, z. B. für den Abschlussbildschirm. */
fun StravaUpload.label(): String = when (this) {
    StravaUpload.Pending -> "Wird zu Strava hochgeladen …"
    is StravaUpload.Retrying -> "Strava: $reason"
    is StravaUpload.Done -> "✓ Auf Strava hochgeladen"
    is StravaUpload.Failed -> "Strava-Upload fehlgeschlagen: $reason"
}
