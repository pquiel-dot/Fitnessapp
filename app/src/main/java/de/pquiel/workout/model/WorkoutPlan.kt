package de.pquiel.workout.model

/** Eine Übung des Plans. Die Farbe ist ein ARGB-Wert, damit das Modell ohne Android auskommt. */
data class Exercise(
    val number: Int,
    val name: String,
    val germanName: String,
    val spokenName: String,
    val tips: List<String>,
    val colorArgb: Long,
)

/** Der Workoutplan: 3 Durchgänge à 4 Übungen (je 45 s), 30 s Pause nach jedem Durchgang außer dem letzten. */
object WorkoutPlan {
    const val ROUNDS = 3
    const val EXERCISE_SECONDS = 45
    const val REST_SECONDS = 30
    const val PREPARE_SECONDS = 10

    val exercises = listOf(
        Exercise(
            number = 1,
            name = "Bodyweight Squats",
            germanName = "Kniebeugen",
            spokenName = "Kniebeugen",
            tips = listOf(
                "Füße schulterbreit, Fußspitzen leicht nach außen",
                "Hüfte nach hinten schieben, Brust bleibt aufrecht",
                "Knie zeigen in Richtung der Fußspitzen",
                "Fersen bleiben am Boden",
            ),
            colorArgb = 0xFFE53935,
        ),
        Exercise(
            number = 2,
            name = "Push-Ups",
            germanName = "Liegestütze",
            spokenName = "Liegestütze",
            tips = listOf(
                "Hände etwas breiter als schulterbreit",
                "Körper bildet eine gerade Linie – Bauch und Po anspannen",
                "Ellenbogen ca. 45° vom Körper weg",
                "Zu schwer? Knie ablegen",
            ),
            colorArgb = 0xFF1E5BC6,
        ),
        Exercise(
            number = 3,
            name = "Reverse Lunges",
            germanName = "Rückwärts-Ausfallschritte",
            spokenName = "Rückwärts-Ausfallschritte",
            tips = listOf(
                "Großer Schritt nach hinten, Beine abwechseln",
                "Vorderes Knie bleibt über dem Fußgelenk",
                "Hinteres Knie Richtung Boden senken",
                "Oberkörper aufrecht, Rumpf fest",
            ),
            colorArgb = 0xFF2E9E44,
        ),
        Exercise(
            number = 4,
            name = "Plank to Shoulder Tap",
            germanName = "Liegestützposition mit Schultertippen",
            spokenName = "Plank mit Schultertippen",
            tips = listOf(
                "Hohe Liegestützposition, Hände unter den Schultern",
                "Abwechselnd mit der Hand die gegenüberliegende Schulter tippen",
                "Hüfte ruhig halten – nicht seitlich kippen",
                "Füße etwas breiter stellen für mehr Stabilität",
            ),
            colorArgb = 0xFFF59E0B,
        ),
    )
}
