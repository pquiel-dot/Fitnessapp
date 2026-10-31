package de.pquiel.workout.model

/**
 * Format der Sicherungsdatei des Verlaufs außerhalb des App-Speichers.
 *
 * Eine Kopfzeile mit `#` und danach je Training eine Zeile aus [WorkoutRecord.encode]. Die Datei
 * bleibt so mit einem Texteditor lesbar und kann von jeder späteren App-Version wieder eingelesen
 * werden.
 */
object HistoryFile {
    const val HEADER = "# Workout Begleiter – Trainingsverlauf (nicht von Hand bearbeiten)"
    const val DEFAULT_NAME = "workout-begleiter-verlauf.txt"

    fun encode(records: List<WorkoutRecord>): String =
        "$HEADER\n" + WorkoutRecord.encodeAll(records.sortedBy { it.finishedAt })

    /**
     * Liest den Inhalt einer Sicherungsdatei. Gibt null zurück, wenn der Text Inhalt hat, aber keine
     * einzige Zeile ein Training ist – dann wurde wohl eine fremde Datei gewählt, die nicht
     * überschrieben werden darf.
     */
    fun decode(text: String): List<WorkoutRecord>? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
        val records = lines.mapNotNull(WorkoutRecord::decode)
        return if (records.isEmpty() && lines.isNotEmpty()) null else records
    }

    /** Vereinigt zwei Verläufe; doppelte Trainings (gleiches Ende) werden nur einmal übernommen. */
    fun merge(current: List<WorkoutRecord>, other: List<WorkoutRecord>): List<WorkoutRecord> =
        (current + other).distinctBy { it.id }.sortedBy { it.finishedAt }
}
