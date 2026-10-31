package de.pquiel.workout

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pquiel.workout.audio.Cues
import de.pquiel.workout.model.Change
import de.pquiel.workout.model.HistoryFile
import de.pquiel.workout.model.Position
import de.pquiel.workout.model.Prepare
import de.pquiel.workout.model.Program
import de.pquiel.workout.model.Rest
import de.pquiel.workout.model.Segment
import de.pquiel.workout.model.Timeline
import de.pquiel.workout.model.Work
import de.pquiel.workout.model.WorkoutRecord
import de.pquiel.workout.strava.Strava
import de.pquiel.workout.strava.StravaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class Status { IDLE, RUNNING, PAUSED, FINISHED }

data class Settings(
    val sound: Boolean = true,
    val voice: Boolean = true,
    val vibration: Boolean = true,
)

data class WorkoutState(
    /** Das ausgewählte bzw. laufende Programm. */
    val program: Program,
    val status: Status,
    val position: Position,
    val elapsedMs: Long,
    val settings: Settings,
    /** Das zuletzt gespeicherte Training, solange der Abschlussbildschirm angezeigt wird. */
    val lastRecord: WorkoutRecord? = null,
)

/** Stand der Sicherungsdatei des Verlaufs. */
data class BackupState(
    /** Name der verbundenen Datei; null, wenn keine verbunden ist. */
    val fileName: String? = null,
    val busy: Boolean = false,
    /** Hinweis oder Fehlermeldung zur letzten Aktion. */
    val message: String? = null,
    val isError: Boolean = false,
) {
    val linked: Boolean get() = fileName != null
}

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val cues = Cues(app)
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val initialProgram = Program.fromName(prefs.getString(KEY_PROGRAM, null)) ?: Program.STRENGTH
    private val historyStore = HistoryStore(app.filesDir)
    private val strava = Strava.get(app)
    val stravaState: StateFlow<StravaState> = strava.state

    private val _history = MutableStateFlow(historyStore.load())
    val history: StateFlow<List<WorkoutRecord>> = _history.asStateFlow()

    private val backup = HistoryBackup(app)
    private val backupLock = Mutex()
    private val _backupState = MutableStateFlow(BackupState(fileName = backup.uri?.let(backup::displayName)))
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _state = MutableStateFlow(
        WorkoutState(
            program = initialProgram,
            status = Status.IDLE,
            position = initialProgram.timeline.positionAt(0),
            elapsedMs = 0,
            settings = Settings(
                sound = prefs.getBoolean(KEY_SOUND, true),
                voice = prefs.getBoolean(KEY_VOICE, true),
                vibration = prefs.getBoolean(KEY_VIBRATION, true),
            ),
        )
    )
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    /** Ablauf des ausgewählten Programms; das Programm wechselt nur, solange kein Training läuft. */
    val timeline: Timeline get() = _state.value.program.timeline

    // Zeitmessung über die monotone Systemuhr, damit der Timer nicht driftet.
    private var accumulatedMs = 0L
    private var runningSince = 0L
    private var ticker: Job? = null
    private var lastIndex = -1
    private var lastSecond = -1

    // Für den Verlauf: Startzeitpunkt, tatsächlich trainierte Zeit und vollständig absolvierte Übungen.
    private var startedAt = 0L
    private var trainedMs = 0L
    private var lastTickAt = 0L
    private var seeked = false
    private val completedWork = mutableSetOf<Int>()

    init {
        applySettings(_state.value.settings)
    }

    fun start() {
        ticker?.cancel()
        accumulatedMs = 0
        lastIndex = -1
        lastSecond = -1
        startedAt = System.currentTimeMillis()
        trainedMs = 0
        seeked = false
        completedWork.clear()
        runningSince = now()
        lastTickAt = runningSince
        _state.update {
            it.copy(status = Status.RUNNING, position = timeline.positionAt(0), elapsedMs = 0, lastRecord = null)
        }
        startTicker()
    }

    fun togglePause() {
        when (_state.value.status) {
            Status.RUNNING -> {
                countTrainedTime()
                accumulatedMs = elapsedNow()
                ticker?.cancel()
                cues.stopSpeaking()
                _state.update { it.copy(status = Status.PAUSED, elapsedMs = accumulatedMs) }
            }
            Status.PAUSED -> {
                runningSince = now()
                lastTickAt = runningSince
                _state.update { it.copy(status = Status.RUNNING) }
                startTicker()
            }
            else -> Unit
        }
    }

    fun skipForward() {
        val pos = timeline.positionAt(elapsedNow())
        if (pos.index >= timeline.lastIndex) {
            seekTo(timeline.totalMs)
        } else {
            seekTo(timeline.startOf(pos.index + 1))
        }
    }

    fun skipBack() {
        val pos = timeline.positionAt(elapsedNow())
        // Innerhalb der ersten Sekunden zurück zum vorherigen Abschnitt, sonst an den Anfang des aktuellen.
        val target = if (pos.elapsedInSegmentMs > 2000 || pos.index == 0) pos.index else pos.index - 1
        seekTo(timeline.startOf(target))
    }

    fun stop() {
        ticker?.cancel()
        cues.stopSpeaking()
        accumulatedMs = 0
        _state.update {
            it.copy(status = Status.IDLE, position = timeline.positionAt(0), elapsedMs = 0, lastRecord = null)
        }
    }

    fun selectProgram(program: Program) {
        if (_state.value.status != Status.IDLE) return
        prefs.edit().putString(KEY_PROGRAM, program.name).apply()
        _state.update { it.copy(program = program, position = program.timeline.positionAt(0), elapsedMs = 0) }
    }

    fun deleteRecord(record: WorkoutRecord) {
        val records = _history.value.filterNot { it.id == record.id }
        setHistory(records)
        strava.forget(record.id)
    }

    /** Legt eine neue Sicherungsdatei an [uri] an und schreibt den bisherigen Verlauf hinein. */
    fun createBackup(uri: Uri) = backupAction {
        backup.link(uri)
        backup.write(uri, _history.value)
        BackupState(fileName = backup.displayName(uri), message = "Verlauf wird ab jetzt in dieser Datei gesichert.")
    }

    /**
     * Verbindet eine vorhandene Sicherungsdatei (z. B. nach einer Neuinstallation), übernimmt ihre
     * Trainings in den Verlauf und hält sie danach auf dem aktuellen Stand.
     */
    fun openBackup(uri: Uri) = backupAction {
        val fromFile = backup.read(uri)
            ?: return@backupAction _backupState.value.copy(
                message = "Die gewählte Datei ist keine Verlaufsdatei des Workout Begleiters.",
                isError = true,
            )
        backup.link(uri)
        val (merged, added) = withContext(Dispatchers.Main) {
            val current = _history.value
            val merged = HistoryFile.merge(current, fromFile)
            historyStore.save(merged)
            _history.value = merged
            merged to merged.size - current.size
        }
        backup.write(uri, merged)
        val message = when (added) {
            0 -> "Datei verbunden – keine neuen Trainings darin."
            1 -> "Datei verbunden – 1 Training wiederhergestellt."
            else -> "Datei verbunden – $added Trainings wiederhergestellt."
        }
        BackupState(fileName = backup.displayName(uri), message = message)
    }

    /** Trennt die Sicherungsdatei; die Datei selbst bleibt erhalten. */
    fun unlinkBackup() {
        backup.unlink()
        _backupState.value = BackupState(message = "Sicherungsdatei getrennt. Die Datei bleibt auf dem Handy erhalten.")
    }

    fun clearBackupMessage() = _backupState.update { it.copy(message = null, isError = false) }

    private fun backupAction(action: suspend () -> BackupState) {
        _backupState.update { it.copy(busy = true, message = null, isError = false) }
        viewModelScope.launch {
            val result = backupLock.withLock {
                try {
                    withContext(Dispatchers.IO) { action() }
                } catch (e: Exception) {
                    _backupState.value.copy(message = "Sicherungsdatei: ${e.message ?: e.javaClass.simpleName}", isError = true)
                }
            }
            _backupState.value = result.copy(busy = false)
        }
    }

    /** Speichert den Verlauf im App-Speicher und – falls verbunden – in der Sicherungsdatei. */
    private fun setHistory(records: List<WorkoutRecord>) {
        historyStore.save(records)
        _history.value = records
        val uri = backup.uri
        if (uri == null) {
            if (_backupState.value.linked) {
                _backupState.value = BackupState(
                    message = "Kein Zugriff mehr auf die Sicherungsdatei. Bitte im Verlauf erneut verbinden.",
                    isError = true,
                )
            }
            return
        }
        viewModelScope.launch {
            backupLock.withLock {
                try {
                    withContext(Dispatchers.IO) { backup.write(uri, records) }
                } catch (e: Exception) {
                    _backupState.update {
                        it.copy(message = "Sicherungsdatei konnte nicht geschrieben werden: ${e.message ?: e.javaClass.simpleName}", isError = true)
                    }
                }
            }
        }
    }

    /** Speichert die Zugangsdaten und gibt die Adresse der Strava-Anmeldung zurück. */
    fun startStravaAuthorization(clientId: String, clientSecret: String): String =
        strava.startAuthorization(clientId, clientSecret)

    /** Verarbeitet die Rückleitung nach der Strava-Anmeldung; gibt false zurück, wenn [uri] keine ist. */
    fun handleStravaRedirect(uri: Uri?): Boolean {
        if (uri == null || !strava.isRedirect(uri)) return false
        viewModelScope.launch { strava.completeAuthorization(uri) }
        return true
    }

    fun disconnectStrava() {
        viewModelScope.launch { strava.disconnect() }
    }

    fun setStravaAutoUpload(enabled: Boolean) = strava.setAutoUpload(enabled)
    fun uploadToStrava(record: WorkoutRecord) = strava.enqueueUpload(record)
    fun clearStravaMessage() = strava.clearMessage()

    fun setSound(enabled: Boolean) = changeSettings { it.copy(sound = enabled) }
    fun setVoice(enabled: Boolean) = changeSettings { it.copy(voice = enabled) }
    fun setVibration(enabled: Boolean) = changeSettings { it.copy(vibration = enabled) }

    private fun changeSettings(change: (Settings) -> Settings) {
        val settings = change(_state.value.settings)
        prefs.edit()
            .putBoolean(KEY_SOUND, settings.sound)
            .putBoolean(KEY_VOICE, settings.voice)
            .putBoolean(KEY_VIBRATION, settings.vibration)
            .apply()
        applySettings(settings)
        _state.update { it.copy(settings = settings) }
    }

    private fun applySettings(settings: Settings) {
        cues.soundEnabled = settings.sound
        cues.voiceEnabled = settings.voice
        cues.vibrationEnabled = settings.vibration
    }

    private fun now() = SystemClock.elapsedRealtime()

    private fun elapsedNow(): Long =
        if (_state.value.status == Status.RUNNING) accumulatedMs + (now() - runningSince) else accumulatedMs

    private fun seekTo(ms: Long) {
        countTrainedTime()
        seeked = true
        accumulatedMs = ms
        if (_state.value.status == Status.RUNNING) runningSince = now()
        refresh()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(100)
            }
        }
    }

    private fun refresh() {
        countTrainedTime()
        val elapsed = elapsedNow()
        val pos = timeline.positionAt(elapsed)
        // Nur Übungen, die bis zum Ende durchlaufen wurden, zählen als absolviert – nicht übersprungene.
        if (!seeked && lastIndex >= 0 && pos.index > lastIndex) {
            for (i in lastIndex until pos.index) if (timeline.segments[i] is Work) completedWork += i
            if (pos.finished && timeline.segments[pos.index] is Work) completedWork += pos.index
        }
        seeked = false
        if (pos.finished) {
            finish()
            return
        }
        val seconds = pos.remainingSeconds
        if (pos.index != lastIndex) {
            onSegmentStart(pos.index, pos.segment)
            lastIndex = pos.index
            lastSecond = seconds
        } else if (seconds != lastSecond) {
            lastSecond = seconds
            if (_state.value.status == Status.RUNNING) onSecond(pos.index, pos.segment, seconds)
        }
        _state.update { it.copy(position = pos, elapsedMs = elapsed) }
    }

    private fun onSegmentStart(index: Int, segment: Segment) {
        if (lastIndex != -1) cues.transition()
        val text = when (segment) {
            is Prepare -> {
                val first = timeline.nextWork(index + 1)
                "Mach dich bereit. Los geht's mit ${first?.exercise?.spokenName ?: "der ersten Übung"}."
            }
            is Work -> {
                val goal = segment.exercise.target?.takeIf { segment.part == 0 }
                if (timeline.segments.getOrNull(index - 1) is Change) {
                    // Übung und Seite wurden schon beim Wechsel angesagt.
                    "Los. ${goal?.let { "$it." } ?: ""}"
                } else {
                    val prefix = if (_state.value.program.rounds > 1 && segment.indexInRound == 0) {
                        "Durchgang ${segment.round + 1}. "
                    } else {
                        ""
                    }
                    val details = listOfNotNull(goal, segment.side).joinToString(". ")
                        .ifEmpty { "${seconds(segment)} Sekunden" }
                    "$prefix${segment.exercise.spokenName}. $details."
                }
            }
            is Rest -> "Pause. ${seconds(segment)} Sekunden durchatmen."
            is Change -> announceChange(segment.next)
        }
        cues.speak(text)
    }

    private fun seconds(segment: Segment) = (segment.durationMs / 1000).toInt()

    private fun announceChange(next: Work): String =
        if (next.part > 0) {
            listOfNotNull("Seitenwechsel", next.side).joinToString(". ") + "."
        } else {
            "Wechsel. Als nächstes ${next.exercise.spokenName}${next.side?.let { ", $it" } ?: ""}."
        }

    private fun onSecond(index: Int, segment: Segment, secondsLeft: Int) {
        when {
            secondsLeft in 1..3 -> cues.tick()
            secondsLeft == 10 && segment is Rest -> {
                val next = timeline.nextWork(index + 1)
                if (next != null) cues.speak("Gleich geht's weiter mit Durchgang ${next.round + 1}: ${next.exercise.spokenName}.")
            }
            secondsLeft == 10 && segment is Work -> {
                val text = when (val next = timeline.segments.getOrNull(index + 1)) {
                    is Work -> "Noch 10 Sekunden. Als nächstes ${next.exercise.spokenName}."
                    is Change -> if (next.next.part > 0) "Noch 10 Sekunden. Danach Seitenwechsel." else "Noch 10 Sekunden."
                    else -> "Noch 10 Sekunden."
                }
                cues.speak(text)
            }
        }
    }

    /** Addiert die seit dem letzten Aufruf vergangene Zeit, sofern das Training läuft (ohne Vorbereitung). */
    private fun countTrainedTime() {
        val t = now()
        val current = _state.value
        if (current.status == Status.RUNNING && current.position.segment !is Prepare) trainedMs += t - lastTickAt
        lastTickAt = t
    }

    private fun finish() {
        ticker?.cancel()
        accumulatedMs = timeline.totalMs
        cues.transition()
        cues.speak("Geschafft! Super gemacht.")
        val record = saveRecord()
        _state.update {
            it.copy(
                status = Status.FINISHED,
                position = timeline.positionAt(timeline.totalMs),
                elapsedMs = timeline.totalMs,
                lastRecord = record,
            )
        }
    }

    /** Speichert das Training im Verlauf, wenn mindestens eine Übung vollständig absolviert wurde. */
    private fun saveRecord(): WorkoutRecord? {
        val counts = timeline.completedExercises(completedWork)
        if (counts.isEmpty()) return null
        val record = WorkoutRecord(
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
            trainedMs = trainedMs,
            exerciseCounts = counts,
            plannedExercises = timeline.plannedExercises,
            program = _state.value.program,
        )
        setHistory(_history.value + record)
        if (strava.state.value.autoUploadActive) strava.enqueueUpload(record)
        return record
    }

    override fun onCleared() {
        cues.release()
    }

    private companion object {
        const val KEY_SOUND = "sound"
        const val KEY_VOICE = "voice"
        const val KEY_VIBRATION = "vibration"
        const val KEY_PROGRAM = "program"
    }
}
