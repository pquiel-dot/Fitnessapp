package de.pquiel.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pquiel.workout.model.HistorySort
import de.pquiel.workout.model.HistorySummary
import de.pquiel.workout.model.WorkoutPlan
import de.pquiel.workout.model.WorkoutRecord
import de.pquiel.workout.model.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)

private fun Long.toLocal() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())

@Composable
fun HistoryScreen(
    records: List<WorkoutRecord>,
    onBack: () -> Unit,
    onDelete: (WorkoutRecord) -> Unit,
) {
    var sort by rememberSaveable { mutableStateOf(HistorySort.NEWEST) }
    var toDelete by remember { mutableStateOf<WorkoutRecord?>(null) }
    val sorted = remember(records, sort) { sort.sort(records) }
    val summary = remember(records) { HistorySummary.of(records) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopBar(sort = sort, onSort = { sort = it }, onBack = onBack, canSort = records.size > 1)
        if (records.isEmpty()) {
            EmptyHistory()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Spacer(Modifier.size(4.dp)) }
                item { SummaryCard(summary) }
                item {
                    Text(
                        "ABGESCHLOSSENE TRAININGS · ${sort.label.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                items(sorted, key = { it.id }) { record ->
                    RecordCard(record, onDelete = { toDelete = record })
                }
                item { Spacer(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) }
            }
        }
    }

    toDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Training löschen?") },
            text = {
                val t = record.finishedAt.toLocal()
                Text("Das Training vom ${t.format(dateFormat)} um ${t.format(timeFormat)} Uhr wird aus dem Verlauf entfernt.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(record)
                    toDelete = null
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun TopBar(sort: HistorySort, onSort: (HistorySort) -> Unit, onBack: () -> Unit, canSort: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = Color.White)
        }
        Text(
            "VERLAUF",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        if (canSort) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sortieren", tint = Color.White)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    HistorySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onSort(option)
                                menuOpen = false
                            },
                            leadingIcon = {
                                if (option == sort) Icon(Icons.Filled.Check, contentDescription = "Ausgewählt")
                                else Spacer(Modifier.size(24.dp))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.History, contentDescription = null, tint = Navy.copy(alpha = 0.4f), modifier = Modifier.size(96.dp))
        Spacer(Modifier.size(16.dp))
        Text("Noch keine Trainings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy)
        Spacer(Modifier.size(8.dp))
        Text(
            "Sobald du ein Training abschließt, wird es hier mit Datum und Uhrzeit gespeichert.",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
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
private fun SummaryCard(summary: HistorySummary) {
    WhiteCard {
        Text("GESAMT", fontWeight = FontWeight.Black, color = Navy)
        Row(Modifier.fillMaxWidth()) {
            Stat("Trainings", summary.workouts.toString(), Modifier.weight(1f))
            Stat("Trainingszeit", formatDuration(summary.trainedMs), Modifier.weight(1f))
            Stat("Übungen", summary.completedExercises.toString(), Modifier.weight(1f))
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("Absolvierte Übungen", fontWeight = FontWeight.Bold, color = Navy)
        WorkoutPlan.exercises.forEach { ex ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                NumberBadge(ex.number, ex.color, size = 24.dp)
                Spacer(Modifier.width(10.dp))
                Text(ex.germanName, modifier = Modifier.weight(1f))
                Text("${summary.exerciseCounts[ex.number] ?: 0}×", fontWeight = FontWeight.Bold, color = ex.color)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Navy)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecordCard(record: WorkoutRecord, onDelete: () -> Unit) {
    val start = record.startedAt.toLocal()
    val end = record.finishedAt.toLocal()
    val complete = record.completedExercises >= record.plannedExercises
    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(end.format(dateFormat), fontWeight = FontWeight.Black, color = Navy)
                Text("${start.format(timeFormat)} – ${end.format(timeFormat)} Uhr", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Training löschen", tint = Navy.copy(alpha = 0.6f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Stat("Dauer", formatDuration(record.trainedMs), Modifier.weight(1f))
            Stat("Übungen", "${record.completedExercises}/${record.plannedExercises}", Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            WorkoutPlan.exercises.forEach { ex ->
                val count = record.exerciseCounts[ex.number] ?: 0
                NumberBadge(ex.number, if (count > 0) ex.color else Color.LightGray, size = 24.dp)
                Text(" ${count}×", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
            }
        }
        if (!complete) {
            Text(
                "${record.plannedExercises - record.completedExercises} Übung(en) übersprungen",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB45309),
            )
        }
    }
}
