package de.pquiel.workout.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pquiel.workout.Settings
import de.pquiel.workout.model.Exercise
import de.pquiel.workout.model.WorkoutPlan
import de.pquiel.workout.model.formatClock

@Composable
fun HomeScreen(
    settings: Settings,
    workoutMs: Long,
    onStart: () -> Unit,
    onSound: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Training starten", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header() }
            item {
                SectionCard {
                    Bullet("Jede Übung ${WorkoutPlan.EXERCISE_SECONDS} Sekunden")
                    Bullet("Nach allen ${WorkoutPlan.exercises.size} Übungen ${WorkoutPlan.REST_SECONDS} Sekunden Pause")
                    Bullet("Insgesamt ${WorkoutPlan.ROUNDS} Durchgänge")
                    Bullet("Saubere Technik hat immer Vorrang vor Tempo")
                }
            }
            item {
                Text(
                    "ÜBUNGEN PRO DURCHGANG",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            items(WorkoutPlan.exercises) { ExerciseCard(it) }
            item { ScheduleCard(workoutMs) }
            item {
                SectionCard {
                    Text("Signale", fontWeight = FontWeight.Bold, color = Navy)
                    SettingRow("Pieptöne", settings.sound, onSound)
                    SettingRow("Sprachansagen", settings.voice, onVoice)
                    SettingRow("Vibration", settings.vibration, onVibration)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("WORKOUTPLAN", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text(
            "${WorkoutPlan.ROUNDS} DURCHGÄNGE · CA. 10 MINUTEN",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•  ", color = Navy, fontWeight = FontWeight.Bold)
        Text(text)
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberBadge(exercise.number, exercise.color)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(exercise.name.uppercase(), fontWeight = FontWeight.Black, color = Navy)
                    Text(exercise.germanName, style = MaterialTheme.typography.bodyMedium)
                }
                Text("${WorkoutPlan.EXERCISE_SECONDS} s", fontWeight = FontWeight.Bold, color = exercise.color)
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Tipps ausblenden" else "Tipps anzeigen",
                    tint = Navy,
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercise.tips.forEach { tip ->
                        Row {
                            Text("✓  ", color = exercise.color, fontWeight = FontWeight.Bold)
                            Text(tip, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(workoutMs: Long) {
    SectionCard {
        Text("ZEITPLAN IM ÜBERBLICK", fontWeight = FontWeight.Black, color = Navy)
        val roundMs = WorkoutPlan.exercises.size * WorkoutPlan.EXERCISE_SECONDS * 1000L
        for (round in 1..WorkoutPlan.ROUNDS) {
            ScheduleRow("Durchgang $round (${WorkoutPlan.exercises.size} Übungen)", formatClock(roundMs) + " min")
            if (round < WorkoutPlan.ROUNDS) {
                ScheduleRow("Pause", formatClock(WorkoutPlan.REST_SECONDS * 1000L) + " min", RestGreen)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Gesamtdauer", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(formatClock(workoutMs) + " min", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Navy)
        }
    }
}

@Composable
private fun ScheduleRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = color, modifier = Modifier.weight(1f))
        Text(value, color = color)
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
