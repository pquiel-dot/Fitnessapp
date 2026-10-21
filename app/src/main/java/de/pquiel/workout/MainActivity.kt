package de.pquiel.workout

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.pquiel.workout.ui.DoneScreen
import de.pquiel.workout.ui.HistoryScreen
import de.pquiel.workout.ui.HomeScreen
import de.pquiel.workout.ui.WorkoutScreen
import de.pquiel.workout.ui.WorkoutTheme

class MainActivity : ComponentActivity() {
    private val vm: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Rückleitung von der Strava-Anmeldung, falls die App dafür neu gestartet wurde.
        if (savedInstanceState == null) vm.handleStravaRedirect(intent?.data)
        setContent {
            WorkoutTheme {
                WorkoutApp(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.handleStravaRedirect(intent.data)
    }
}

@Composable
fun WorkoutApp(vm: WorkoutViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    val strava by vm.stravaState.collectAsState()
    val context = LocalContext.current
    var showHistory by rememberSaveable { mutableStateOf(false) }

    if (showHistory && state.status != Status.RUNNING && state.status != Status.PAUSED) {
        BackHandler { showHistory = false }
        HistoryScreen(
            records = history,
            strava = strava,
            onBack = { showHistory = false },
            onDelete = vm::deleteRecord,
            onUpload = vm::uploadToStrava,
        )
        return
    }

    when (state.status) {
        Status.IDLE -> HomeScreen(
            program = state.program,
            onSelectProgram = vm::selectProgram,
            settings = state.settings,
            workoutMs = vm.timeline.workoutMs,
            completedWorkouts = history.size,
            onStart = vm::start,
            onHistory = { showHistory = true },
            onSound = vm::setSound,
            onVoice = vm::setVoice,
            onVibration = vm::setVibration,
            strava = strava,
            onStravaConnect = { id, secret ->
                val url = vm.startStravaAuthorization(id, secret)
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Kein Browser für die Strava-Anmeldung gefunden", Toast.LENGTH_LONG).show()
                }
            },
            onStravaDisconnect = vm::disconnectStrava,
            onStravaAutoUpload = vm::setStravaAutoUpload,
            onStravaDismissMessage = vm::clearStravaMessage,
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
            program = state.program,
            workoutMs = vm.timeline.workoutMs,
            record = state.lastRecord,
            stravaUpload = state.lastRecord?.let { strava.uploads[it.id] },
            onRestart = vm::start,
            onHome = vm::stop,
            onHistory = { showHistory = true },
        )
    }
}
