package de.pquiel.workout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.pquiel.workout.Status
import de.pquiel.workout.WorkoutState
import de.pquiel.workout.model.Prepare
import de.pquiel.workout.model.Rest
import de.pquiel.workout.model.Segment
import de.pquiel.workout.model.Timeline
import de.pquiel.workout.model.Work
import de.pquiel.workout.model.WorkoutPlan
import de.pquiel.workout.model.formatClock

@Composable
fun WorkoutScreen(
    state: WorkoutState,
    timeline: Timeline,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
) {
    // Bildschirm während des Trainings anlassen.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var confirmStop by remember { mutableStateOf(false) }
    BackHandler { confirmStop = true }

    val pos = state.position
    val segment = pos.segment
    val paused = state.status == Status.PAUSED
    val accent by animateColorAsState(segment.accentColor(), label = "accent")
    val background by animateColorAsState(
        if (segment is Rest) RestGreenLight else accent.copy(alpha = 0.10f).compositeOverWhite(),
        label = "background",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(state, timeline, onClose = { confirmStop = true })

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.height(12.dp))
                SegmentHeader(segment, timeline, pos.index, accent)
                Spacer(Modifier.height(20.dp))
                CountdownRing(
                    fraction = pos.remainingInSegmentMs.toFloat() / segment.durationMs,
                    color = accent,
                    modifier = Modifier.size(250.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            pos.remainingSeconds.toString(),
                            fontSize = 96.sp,
                            fontWeight = FontWeight.Black,
                            color = Navy,
                        )
                        Text(
                            if (paused) "PAUSIERT" else "Sekunden",
                            fontWeight = FontWeight.Bold,
                            color = if (paused) accent else Navy.copy(alpha = 0.6f),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                TipsCard(segment, timeline, pos.index, accent)
                Spacer(Modifier.height(12.dp))
            }

            NextUp(timeline.segments.getOrNull(pos.index + 1))
            Spacer(Modifier.height(12.dp))
            Controls(paused, accent, onTogglePause, onNext, onPrevious)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Training beenden?") },
            text = { Text("Dein Fortschritt in dieser Einheit geht verloren.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    onStop()
                }) { Text("Beenden") }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("Weiter trainieren") }
            },
        )
    }
}

private fun Color.compositeOverWhite(): Color =
    Color(
        red = red * alpha + (1 - alpha),
        green = green * alpha + (1 - alpha),
        blue = blue * alpha + (1 - alpha),
    )

@Composable
private fun TopBar(state: WorkoutState, timeline: Timeline, onClose: () -> Unit) {
    val segment = state.position.segment
    val roundLabel = when (segment) {
        is Prepare -> "Gleich geht's los"
        is Work -> "Durchgang ${segment.round + 1} von ${WorkoutPlan.ROUNDS}"
        is Rest -> "Pause nach Durchgang ${segment.afterRound + 1}"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Training beenden", tint = Navy)
        }
        Text(
            roundLabel,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Navy,
        )
        Text(
            formatClock(timeline.workoutRemainingMs(state.elapsedMs)),
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
    LinearProgressIndicator(
        progress = { timeline.workoutProgress(state.elapsedMs) },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = Navy,
        trackColor = Navy.copy(alpha = 0.12f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

@Composable
private fun SegmentHeader(segment: Segment, timeline: Timeline, index: Int, accent: Color) {
    when (segment) {
        is Work -> {
            RoundDots(current = segment.indexInRound)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberBadge(segment.exercise.number, segment.exercise.color, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "ÜBUNG ${segment.indexInRound + 1} VON ${WorkoutPlan.exercises.size}",
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Spacer(Modifier.height(8.dp))
            Title(segment.exercise.name.uppercase())
            Subtitle(segment.exercise.germanName)
        }
        is Rest -> {
            Text("PAUSE", fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(8.dp))
            Title("DURCHATMEN")
            Subtitle("Durchgang ${segment.afterRound + 1} geschafft!")
        }
        is Prepare -> {
            val first = timeline.nextWork(index + 1)
            Text("MACH DICH BEREIT", fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(8.dp))
            Title(first?.exercise?.name?.uppercase() ?: "LOS GEHT'S")
            Subtitle(first?.exercise?.germanName ?: "")
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Black,
        color = Navy,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Subtitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = Navy.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
    )
}

/** Zeigt die vier Übungen eines Durchgangs; erledigte und aktuelle sind gefüllt. */
@Composable
private fun RoundDots(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WorkoutPlan.exercises.forEachIndexed { i, ex ->
            val modifier = Modifier
                .size(if (i == current) 16.dp else 12.dp)
                .let {
                    if (i <= current) it.background(ex.color, CircleShape)
                    else it.border(2.dp, ex.color, CircleShape)
                }
            Box(modifier)
        }
    }
}

@Composable
private fun TipsCard(segment: Segment, timeline: Timeline, index: Int, accent: Color) {
    val tips = when (segment) {
        is Work -> segment.exercise.tips
        is Prepare -> timeline.nextWork(index + 1)?.exercise?.tips.orEmpty()
        is Rest -> listOf("Ruhig und tief atmen", "Einen Schluck Wasser trinken", "Locker ausschütteln")
    }
    if (tips.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tips.forEach { tip ->
            Row {
                Text("✓  ", color = accent, fontWeight = FontWeight.Bold)
                Text(tip, color = Navy)
            }
        }
    }
}

@Composable
private fun NextUp(next: Segment?) {
    val text = when (next) {
        null -> "Letzte Übung – gib alles!"
        is Work -> "Als nächstes: ${next.exercise.name}"
        is Rest -> "Als nächstes: ${WorkoutPlan.REST_SECONDS} Sekunden Pause"
        is Prepare -> ""
    }
    Text(text, fontWeight = FontWeight.SemiBold, color = Navy.copy(alpha = 0.8f))
}

@Composable
private fun Controls(
    paused: Boolean,
    accent: Color,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Zurück", tint = Navy, modifier = Modifier.size(40.dp))
        }
        FilledIconButton(
            onClick = onTogglePause,
            modifier = Modifier.size(88.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color.White),
        ) {
            Icon(
                if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (paused) "Fortsetzen" else "Pausieren",
                modifier = Modifier.size(48.dp),
            )
        }
        IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Überspringen", tint = Navy, modifier = Modifier.size(40.dp))
        }
    }
}
