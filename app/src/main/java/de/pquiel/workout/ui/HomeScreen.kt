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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pquiel.workout.Settings
import de.pquiel.workout.model.Change
import de.pquiel.workout.model.Exercise
import de.pquiel.workout.model.Program
import de.pquiel.workout.model.Rest
import de.pquiel.workout.model.Work
import de.pquiel.workout.model.WorkoutPlan
import de.pquiel.workout.model.formatClock
import de.pquiel.workout.strava.StravaState

@Composable
fun HomeScreen(
    program: Program,
    onSelectProgram: (Program) -> Unit,
    settings: Settings,
    workoutMs: Long,
    completedWorkouts: Int,
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onSound: (Boolean) -> Unit,
    onVoice: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    strava: StravaState,
    onStravaConnect: (clientId: String, clientSecret: String) -> Unit,
    onStravaDisconnect: () -> Unit,
    onStravaAutoUpload: (Boolean) -> Unit,
    onStravaDismissMessage: () -> Unit,
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
            item { Header(program, onSelectProgram, completedWorkouts, onHistory) }
            item {
                SectionCard {
                    program.intro.forEach { Bullet(it) }
                }
            }
            item {
                Text(
                    program.exercisesHeading,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            items(program.exercises, key = { "${program.name}-${it.number}" }) { ExerciseCard(it) }
            item { ScheduleCard(program, workoutMs) }
            item {
                SectionCard {
                    Text("Signale", fontWeight = FontWeight.Bold, color = Navy)
                    SettingRow("Pieptöne", settings.sound, onSound)
                    SettingRow("Sprachansagen", settings.voice, onVoice)
                    SettingRow("Vibration", settings.vibration, onVibration)
                }
            }
            item {
                StravaCard(
                    state = strava,
                    onConnect = onStravaConnect,
                    onDisconnect = onStravaDisconnect,
                    onAutoUpload = onStravaAutoUpload,
                    onDismissMessage = onStravaDismissMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Header(
    program: Program,
    onSelectProgram: (Program) -> Unit,
    completedWorkouts: Int,
    onHistory: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 20.dp),
    ) {
        ProgramTabs(program, onSelectProgram)
        Spacer(Modifier.height(20.dp))
        HeaderTitle(program, completedWorkouts, onHistory)
    }
}

/** Auswahl des Programms, z. B. Workoutplan oder Running-Mobilisation. */
@Composable
private fun ProgramTabs(selected: Program, onSelect: (Program) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(end = 12.dp)
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        Program.entries.forEach { p ->
            val isSelected = p == selected
            Text(
                p.label,
                color = if (isSelected) Navy else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSelect(p) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun HeaderTitle(program: Program, completedWorkouts: Int, onHistory: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                program.title,
                color = Color.White,
                fontSize = if (program.title.length > 12) 26.sp else 34.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                program.subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onHistory) {
                Icon(Icons.Filled.History, contentDescription = "Verlauf", tint = Color.White)
            }
            Text(
                if (completedWorkouts == 1) "1 Training" else "$completedWorkouts Trainings",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
            )
        }
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
                Text(exercise.durationLabel(), fontWeight = FontWeight.Bold, color = exercise.color)
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Tipps ausblenden" else "Tipps anzeigen",
                    tint = Navy,
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    exercise.purpose?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Navy)
                    }
                    exercise.target?.let { Text(it, fontWeight = FontWeight.Bold, color = exercise.color) }
                    exercise.parts.mapNotNull { it.side }.takeIf { it.isNotEmpty() }?.let { sides ->
                        Text("Seiten: ${sides.joinToString(" · ")}", style = MaterialTheme.typography.bodySmall)
                    }
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

/** Dauer einer Übung für die Übersicht, z. B. „45 s“ oder „2 × 60 s“. */
private fun Exercise.durationLabel(): String {
    if (parts.isEmpty()) return "${WorkoutPlan.EXERCISE_SECONDS} s"
    val seconds = parts.map { it.seconds }.distinct()
    return if (parts.size > 1 && seconds.size == 1) "${parts.size} × ${seconds.single()} s" else "${parts.sumOf { it.seconds }} s"
}

@Composable
private fun ScheduleCard(program: Program, workoutMs: Long) {
    SectionCard {
        Text("ZEITPLAN IM ÜBERBLICK", fontWeight = FontWeight.Black, color = Navy)
        val segments = program.timeline.segments
        if (program.rounds > 1) {
            for (round in 0 until program.rounds) {
                val roundMs = segments.filterIsInstance<Work>().filter { it.round == round }.sumOf { it.durationMs }
                ScheduleRow("Durchgang ${round + 1} (${program.exercises.size} Übungen)", formatClock(roundMs) + " min")
                segments.filterIsInstance<Rest>().firstOrNull { it.afterRound == round }?.let {
                    ScheduleRow("Pause", formatClock(it.durationMs) + " min", RestGreen)
                }
            }
        } else {
            val works = segments.filterIsInstance<Work>()
            program.exercises.forEach { ex ->
                val ms = works.filter { it.exercise == ex }.sumOf { it.durationMs }
                ScheduleRow("${ex.number}. ${ex.name}", formatClock(ms) + " min")
            }
            val changeMs = segments.filterIsInstance<Change>().sumOf { it.durationMs }
            if (changeMs > 0) ScheduleRow("Wechselzeiten", formatClock(changeMs) + " min", RestGreen)
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
