package de.pquiel.workout.model

/** Ein Abschnitt im Ablauf des Trainings. */
sealed interface Segment {
    val durationMs: Long
}

/** Kurzer Countdown vor dem ersten Durchgang. Zählt nicht zur Trainingsdauer. */
data class Prepare(override val durationMs: Long) : Segment

/** Eine Übung. [round] und [indexInRound] beginnen bei 0. */
data class Work(
    val round: Int,
    val indexInRound: Int,
    val exercise: Exercise,
    override val durationMs: Long,
) : Segment

/** Pause nach dem Durchgang [afterRound] (beginnt bei 0). */
data class Rest(val afterRound: Int, override val durationMs: Long) : Segment

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
    }
}

/** Formatiert Millisekunden als m:ss (Sekunden aufgerundet). */
fun formatClock(ms: Long): String {
    val total = ceilSeconds(ms.coerceAtLeast(0))
    return "%d:%02d".format(total / 60, total % 60)
}
