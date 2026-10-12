package de.pquiel.workout.model

/** Ein Abschnitt im Ablauf des Trainings. */
sealed interface Segment {
    val durationMs: Long
}

/** Kurzer Countdown vor dem ersten Durchgang. Zählt nicht zur Trainingsdauer. */
data class Prepare(override val durationMs: Long) : Segment

/**
 * Eine Übung bzw. ein Abschnitt davon. [round], [indexInRound] und [part] beginnen bei 0;
 * [part] zählt die Abschnitte einer Übung mit mehreren Seiten (siehe [Exercise.parts]).
 */
data class Work(
    val round: Int,
    val indexInRound: Int,
    val exercise: Exercise,
    override val durationMs: Long,
    val part: Int = 0,
) : Segment {
    val side: String? get() = exercise.parts.getOrNull(part)?.side
    val partCount: Int get() = exercise.parts.size.coerceAtLeast(1)
    val isLastPart: Boolean get() = part == partCount - 1
}

/** Pause nach dem Durchgang [afterRound] (beginnt bei 0). */
data class Rest(val afterRound: Int, override val durationMs: Long) : Segment

/** Kurze Wechselzeit, um sich für [next] (nächste Übung oder andere Seite) zu positionieren. */
data class Change(val next: Work, override val durationMs: Long) : Segment

/** Position im Ablauf zu einem bestimmten Zeitpunkt. */
data class Position(
    val index: Int,
    val segment: Segment,
    val elapsedInSegmentMs: Long,
    val finished: Boolean,
) {
    val remainingInSegmentMs: Long get() = (segment.durationMs - elapsedInSegmentMs).coerceAtLeast(0)

    /** Verbleibende ganze Sekunden, aufgerundet (45, 44, … 1). */
    val remainingSeconds: Int get() = ceilSeconds(remainingInSegmentMs)
}

fun ceilSeconds(ms: Long): Int = ((ms + 999) / 1000).toInt()

class Timeline(val segments: List<Segment>) {
    init {
        require(segments.isNotEmpty()) { "Timeline braucht mindestens einen Abschnitt" }
    }

    private val starts: LongArray = LongArray(segments.size).also { arr ->
        var acc = 0L
        segments.forEachIndexed { i, s ->
            arr[i] = acc
            acc += s.durationMs
        }
    }

    val totalMs: Long = segments.sumOf { it.durationMs }

    /** Dauer des eigentlichen Trainings (ohne Vorbereitungs-Countdown). */
    val workoutMs: Long = segments.filterNot { it is Prepare }.sumOf { it.durationMs }

    private val prepareMs: Long = totalMs - workoutMs

    val lastIndex: Int get() = segments.lastIndex

    fun startOf(index: Int): Long = starts[index]

    fun positionAt(elapsedMs: Long): Position {
        val t = elapsedMs.coerceAtLeast(0)
        if (t >= totalMs) {
            val last = segments.last()
            return Position(segments.lastIndex, last, last.durationMs, finished = true)
        }
        var index = starts.indexOfLast { it <= t }
        if (index < 0) index = 0
        return Position(index, segments[index], t - starts[index], finished = false)
    }

    /** Verbleibende Trainingszeit (ohne Vorbereitung). */
    fun workoutRemainingMs(elapsedMs: Long): Long =
        (workoutMs - (elapsedMs - prepareMs).coerceAtLeast(0)).coerceIn(0, workoutMs)

    /** Fortschritt des Trainings von 0 bis 1 (ohne Vorbereitung). */
    fun workoutProgress(elapsedMs: Long): Float =
        if (workoutMs == 0L) 1f else 1f - workoutRemainingMs(elapsedMs).toFloat() / workoutMs

    /** Die nächste Übung ab (einschließlich) Index [fromIndex], oder null. */
    fun nextWork(fromIndex: Int): Work? =
        segments.drop(fromIndex.coerceAtLeast(0)).firstOrNull { it is Work } as Work?

    // Indizes der Abschnitte jeder Übung je Durchgang (eine Übung kann aus mehreren Seiten bestehen).
    private val exerciseGroups: List<List<Int>> =
        segments.indices.filter { segments[it] is Work }
            .groupBy { (segments[it] as Work).let { w -> w.round to w.indexInRound } }
            .values.toList()

    /** Anzahl der geplanten Übungen über alle Durchgänge. */
    val plannedExercises: Int get() = exerciseGroups.size

    /**
     * Ordnet jeder Übungsnummer zu, wie oft sie vollständig absolviert wurde, wenn die Abschnitte mit den
     * Indizes [completedSegments] absolviert sind. Eine Übung zählt nur, wenn alle ihre Seiten absolviert sind.
     */
    fun completedExercises(completedSegments: Set<Int>): Map<Int, Int> =
        exerciseGroups.filter { group -> group.all { it in completedSegments } }
            .map { (segments[it.first()] as Work).exercise.number }
            .groupingBy { it }
            .eachCount()

    companion object {
        fun standard(
            exercises: List<Exercise> = WorkoutPlan.exercises,
            rounds: Int = WorkoutPlan.ROUNDS,
            exerciseSeconds: Int = WorkoutPlan.EXERCISE_SECONDS,
            restSeconds: Int = WorkoutPlan.REST_SECONDS,
            prepareSeconds: Int = WorkoutPlan.PREPARE_SECONDS,
        ): Timeline {
            val list = mutableListOf<Segment>()
            if (prepareSeconds > 0) list += Prepare(prepareSeconds * 1000L)
            for (round in 0 until rounds) {
                exercises.forEachIndexed { i, ex -> list += Work(round, i, ex, exerciseSeconds * 1000L) }
                if (round < rounds - 1 && restSeconds > 0) list += Rest(round, restSeconds * 1000L)
            }
            return Timeline(list)
        }

        /**
         * Alle Übungen nacheinander, jede mit ihren [Exercise.parts]. Vor jeder neuen Übung liegt eine
         * Wechselzeit von [changeSeconds], vor jeder weiteren Seite eine von [sideChangeSeconds].
         */
        fun sequence(
            exercises: List<Exercise>,
            prepareSeconds: Int,
            changeSeconds: Int,
            sideChangeSeconds: Int,
        ): Timeline {
            val list = mutableListOf<Segment>()
            if (prepareSeconds > 0) list += Prepare(prepareSeconds * 1000L)
            exercises.forEachIndexed { i, ex ->
                ex.parts.forEachIndexed { p, part ->
                    val work = Work(0, i, ex, part.seconds * 1000L, part = p)
                    val change = if (p == 0) changeSeconds else sideChangeSeconds
                    if (list.lastOrNull() is Work && change > 0) list += Change(work, change * 1000L)
                    list += work
                }
            }
            return Timeline(list)
        }
    }
}

/** Formatiert Millisekunden als m:ss (Sekunden aufgerundet). */
fun formatClock(ms: Long): String {
    val total = ceilSeconds(ms.coerceAtLeast(0))
    return "%d:%02d".format(total / 60, total % 60)
}
