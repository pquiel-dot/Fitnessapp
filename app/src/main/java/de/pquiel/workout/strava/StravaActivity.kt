package de.pquiel.workout.strava

import de.pquiel.workout.model.Program
import de.pquiel.workout.model.WorkoutRecord
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Eine manuelle Strava-Aktivität, wie sie `POST /api/v3/activities` als Formularfelder erwartet. */
data class StravaActivity(
    val name: String,
    val sportType: String,
    /** Lokale Startzeit ohne Zeitzone, z. B. `2026-10-21T07:30:00`. */
    val startDateLocal: String,
    val elapsedSeconds: Long,
    val description: String,
) {
    fun formParams(): Map<String, String> = linkedMapOf(
        "name" to name,
        "sport_type" to sportType,
        "start_date_local" to startDateLocal,
        "elapsed_time" to elapsedSeconds.toString(),
        "description" to description,
    )

    companion object {
        private val localFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        /** Baut die Aktivität für ein gespeichertes Training; die Dauer ist die tatsächlich trainierte Zeit. */
        fun of(record: WorkoutRecord, zone: ZoneId = ZoneId.systemDefault()): StravaActivity =
            StravaActivity(
                name = record.program.label,
                sportType = sportTypeOf(record.program),
                startDateLocal = Instant.ofEpochMilli(record.startedAt).atZone(zone).format(localFormat),
                elapsedSeconds = ((record.trainedMs + 500) / 1000).coerceAtLeast(1),
                description = describe(record),
            )

        /** Strava-Sportart: „Krafttraining“ für den Workoutplan, „Training“ für die Mobilisation. */
        fun sportTypeOf(program: Program): String = when (program) {
            Program.STRENGTH -> "WeightTraining"
            Program.MOBILITY -> "Workout"
        }

        private fun describe(record: WorkoutRecord): String {
            val program = record.program
            val lines = mutableListOf<String>()
            lines += "${record.completedExercises} von ${record.plannedExercises} Übungen absolviert"
            lines += ""
            program.exercises.forEach { ex ->
                val count = record.exerciseCounts[ex.number] ?: 0
                lines += if (program.rounds > 1) {
                    "${ex.number}. ${ex.name} (${ex.germanName}): $count×"
                } else {
                    "${if (count > 0) "✓" else "–"} ${ex.number}. ${ex.name} (${ex.germanName})"
                }
            }
            lines += ""
            lines += "Aufgezeichnet mit Workout Begleiter"
            return lines.joinToString("\n")
        }
    }
}

/** URLs und Kodierung für den OAuth-Ablauf mit Strava. */
object StravaAuth {
    /** Strava lässt `localhost` als Callback-Domain zu; die App fängt das eigene Schema ab. */
    const val REDIRECT_URI = "workoutbegleiter://localhost/strava"

    /** Benötigt wird nur das Anlegen von Aktivitäten; `read` liefert den Namen des Athleten. */
    const val SCOPE = "read,activity:write"
    const val WRITE_SCOPE = "activity:write"

    private const val AUTHORIZE_URL = "https://www.strava.com/oauth/mobile/authorize"

    fun authorizeUrl(clientId: String, state: String): String =
        AUTHORIZE_URL + "?" + formEncode(
            linkedMapOf(
                "client_id" to clientId,
                "redirect_uri" to REDIRECT_URI,
                "response_type" to "code",
                "approval_prompt" to "auto",
                "scope" to SCOPE,
                "state" to state,
            )
        )

    /** Prüft, ob die vom Nutzer erteilten Berechtigungen das Hochladen erlauben. */
    fun canWrite(grantedScope: String?): Boolean =
        grantedScope?.split(",")?.map { it.trim() }?.contains(WRITE_SCOPE) == true

    fun formEncode(params: Map<String, String>): String =
        params.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
}
