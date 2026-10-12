package de.pquiel.workout.model

/**
 * Ein abgeschlossenes Training des Programms [program].
 *
 * Zeitpunkte sind Epoch-Millisekunden (Wanduhr). [trainedMs] ist die tatsächlich trainierte Zeit
 * ohne Vorbereitungs-Countdown und ohne Pausen über den Pause-Knopf.
 * [exerciseCounts] ordnet jeder Übungsnummer zu, wie oft sie vollständig absolviert wurde
 * (übersprungene Übungen zählen nicht).
 */
data class WorkoutRecord(
    val startedAt: Long,
    val finishedAt: Long,
    val trainedMs: Long,
    val exerciseCounts: Map<Int, Int>,
    val plannedExercises: Int,
    val program: Program = Program.STRENGTH,
) {
    val completedExercises: Int get() = exerciseCounts.values.sum()

    /** Eindeutiger Schlüssel, z. B. zum Löschen. */
    val id: Long get() = finishedAt

    /** Eine Zeile im Speicherformat: `startedAt;finishedAt;trainedMs;planned;nr:anzahl,nr:anzahl;PROGRAMM`. */
    fun encode(): String {
        val counts = exerciseCounts.toSortedMap().entries.joinToString(",") { "${it.key}:${it.value}" }
        return "$startedAt;$finishedAt;$trainedMs;$plannedExercises;$counts;${program.name}"
    }

    companion object {
        /**
         * Liest eine Zeile aus [encode]; ungültige Zeilen ergeben null.
         * Ältere Zeilen ohne Programm (5 Felder) gehören zum Workoutplan.
         */
        fun decode(line: String): WorkoutRecord? {
            val parts = line.trim().split(";")
            if (parts.size != 5 && parts.size != 6) return null
            val program = if (parts.size == 6) Program.fromName(parts[5]) ?: return null else Program.STRENGTH
            val counts = if (parts[4].isEmpty()) emptyMap() else parts[4].split(",").associate { pair ->
                val (nr, count) = pair.split(":").takeIf { it.size == 2 } ?: return null
                (nr.toIntOrNull() ?: return null) to (count.toIntOrNull() ?: return null)
            }
            return WorkoutRecord(
                startedAt = parts[0].toLongOrNull() ?: return null,
                finishedAt = parts[1].toLongOrNull() ?: return null,
                trainedMs = parts[2].toLongOrNull() ?: return null,
                plannedExercises = parts[3].toIntOrNull() ?: return null,
                exerciseCounts = counts,
                program = program,
            )
        }

        fun decodeAll(text: String): List<WorkoutRecord> =
            text.lineSequence().filter { it.isNotBlank() }.mapNotNull(::decode).toList()

        fun encodeAll(records: List<WorkoutRecord>): String =
            records.joinToString("\n", postfix = if (records.isEmpty()) "" else "\n") { it.encode() }
    }
}

/** Sortierungen für die Trainingsübersicht. */
enum class HistorySort(val label: String, private val comparator: Comparator<WorkoutRecord>) {
    NEWEST("Neueste zuerst", compareByDescending { it.finishedAt }),
    OLDEST("Älteste zuerst", compareBy { it.finishedAt }),
    LONGEST("Längste Dauer", compareByDescending<WorkoutRecord> { it.trainedMs }.thenByDescending { it.finishedAt }),
    SHORTEST("Kürzeste Dauer", compareBy<WorkoutRecord> { it.trainedMs }.thenByDescending { it.finishedAt }),
    MOST_EXERCISES(
        "Meiste Übungen",
        compareByDescending<WorkoutRecord> { it.completedExercises }.thenByDescending { it.finishedAt },
    ),
    FEWEST_EXERCISES(
        "Wenigste Übungen",
        compareBy<WorkoutRecord> { it.completedExercises }.thenByDescending { it.finishedAt },
    );

    fun sort(records: List<WorkoutRecord>): List<WorkoutRecord> = records.sortedWith(comparator)
}

/** Zusammenfassung über alle gespeicherten Trainings. */
data class HistorySummary(
    val workouts: Int,
    val trainedMs: Long,
    val completedExercises: Int,
    /** Je Programm: wie oft jede Übungsnummer absolviert wurde. Nur Programme mit Trainings sind enthalten. */
    val exerciseCounts: Map<Program, Map<Int, Int>>,
) {
    companion object {
        fun of(records: List<WorkoutRecord>): HistorySummary {
            val counts = mutableMapOf<Program, MutableMap<Int, Int>>()
            records.forEach { r ->
                val map = counts.getOrPut(r.program) { mutableMapOf() }
                r.exerciseCounts.forEach { (nr, c) -> map[nr] = (map[nr] ?: 0) + c }
            }
            return HistorySummary(
                workouts = records.size,
                trainedMs = records.sumOf { it.trainedMs },
                completedExercises = records.sumOf { it.completedExercises },
                exerciseCounts = counts,
            )
        }
    }
}

/** Formatiert eine Dauer als m:ss bzw. h:mm:ss, auf ganze Sekunden gerundet. */
fun formatDuration(ms: Long): String {
    val total = ((ms.coerceAtLeast(0) + 500) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
