package de.pquiel.workout

import android.util.AtomicFile
import de.pquiel.workout.model.WorkoutRecord
import java.io.File

/** Speichert abgeschlossene Trainings als Textdatei im internen App-Speicher. */
class HistoryStore(dir: File) {
    private val file = AtomicFile(File(dir, "history.txt"))

    fun load(): List<WorkoutRecord> =
        try {
            WorkoutRecord.decodeAll(file.readFully().decodeToString())
        } catch (e: Exception) {
            emptyList()
        }

    fun save(records: List<WorkoutRecord>) {
        val out = file.startWrite()
        try {
            out.write(WorkoutRecord.encodeAll(records).encodeToByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
        }
    }
}
