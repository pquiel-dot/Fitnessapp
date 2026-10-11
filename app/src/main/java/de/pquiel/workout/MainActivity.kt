package de.pquiel.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pquiel.workout.ui.DoneScreen
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
    when (state.status) {
        Status.IDLE -> HomeScreen(
            settings = state.settings,
            workoutMs = vm.timeline.workoutMs,
            onStart = vm::start,
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
            onRestart = vm::start,
            onHome = vm::stop,
        )
    }
}
