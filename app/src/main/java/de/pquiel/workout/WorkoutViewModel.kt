package de.pquiel.workout

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pquiel.workout.audio.Cues
import de.pquiel.workout.model.Change
import de.pquiel.workout.model.Position
import de.pquiel.workout.model.Prepare
import de.pquiel.workout.model.Program
import de.pquiel.workout.model.Rest
import de.pquiel.workout.model.Segment
import de.pquiel.workout.model.Timeline
import de.pquiel.workout.model.Work
import de.pquiel.workout.model.WorkoutRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

class WorkoutViewModel(app: Application) : AndroidViewModel(app) {
    private val cues = Cues(app)
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val initialProgram = Program.fromName(prefs.getString(KEY_PROGRAM, null)) ?: Program.STRENGTH
    private val historyStore = HistoryStore(app.filesDir)

    private val _history = MutableStateFlow(historyStore.load())
    val history: StateFlow<List<WorkoutRecord>> = _history.asStateFlow()

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
        historyStore.save(records)
        _history.value = records
    }

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
        val records = _history.value + record
        historyStore.save(records)
        _history.value = records
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
