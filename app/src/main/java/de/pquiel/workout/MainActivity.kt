package de.pquiel.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pquiel.workout.ui.DoneScreen
import de.pquiel.workout.ui.HistoryScreen
import de.pquiel.workout.ui.HomeScreen
import de.pquiel.workout.ui.WorkoutScreen
import de.pquiel.workout.ui.WorkoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutTheme {
                WorkoutApp()
            }
        }
    }
}

@Composable
fun WorkoutApp(vm: WorkoutViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    var showHistory by rememberSaveable { mutableStateOf(false) }

    if (showHistory && state.status != Status.RUNNING && state.status != Status.PAUSED) {
        BackHandler { showHistory = false }
        HistoryScreen(
            records = history,
            onBack = { showHistory = false },
            onDelete = vm::deleteRecord,
        )
        return
    }

    when (state.status) {
        Status.IDLE -> HomeScreen(
            settings = state.settings,
            workoutMs = vm.timeline.workoutMs,
            completedWorkouts = history.size,
            onStart = vm::start,
            onHistory = { showHistory = true },
            onSound = vm::setSound,
            onVoice = vm::setVoice,
            onVibration = vm::setVibration,
        )
        Status.RUNNING, Status.PAUSED -> WorkoutScreen(
            state = state,
            timeline = vm.timeline,
            onTogglePause = vm::togglePause,
            onNext = vm::skipForward,
            onPrevious = vm::skipBack,
            onStop = vm::stop,
        )
        Status.FINISHED -> DoneScreen(
            workoutMs = vm.timeline.workoutMs,
            record = state.lastRecord,
            onRestart = vm::start,
            onHome = vm::stop,
            onHistory = { showHistory = true },
        )
    }
}
