package de.pquiel.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pquiel.workout.model.Program
import de.pquiel.workout.model.WorkoutRecord
import de.pquiel.workout.model.formatDuration
import de.pquiel.workout.model.formatClock
import de.pquiel.workout.strava.StravaUpload

@Composable
fun DoneScreen(
    program: Program,
    workoutMs: Long,
    record: WorkoutRecord?,
    stravaUpload: StravaUpload?,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onHistory: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = RestGreenLight,
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(24.dp))
        Text("GESCHAFFT!", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        val total = program.timeline.plannedExercises
        Text(
            if (record == null || record.completedExercises >= total) {
                if (program.rounds > 1) "${program.rounds} Durchgänge · $total Übungen" else "${program.label} · $total Übungen"
            } else {
                "${record.completedExercises} von $total Übungen absolviert"
            },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text("GESAMTDAUER", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        Text(
            record?.let { formatDuration(it.trainedMs) } ?: formatClock(workoutMs),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
        )
        Text("MINUTEN", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        if (record != null) {
            Spacer(Modifier.height(12.dp))
            Text("✓ Im Verlauf gespeichert", color = RestGreenLight, fontWeight = FontWeight.SemiBold)
        }
        stravaUpload?.let { upload ->
            Spacer(Modifier.height(4.dp))
            Text(
                upload.label(),
                color = if (upload is StravaUpload.Done) RestGreenLight else Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy),
        ) {
            Text("Nochmal trainieren", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text("Zur Übersicht", fontSize = 18.sp)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onHistory) {
            Text("Verlauf ansehen", fontSize = 16.sp, color = Color.White)
        }
    }
}
