package de.pquiel.workout.model

/** Die Trainingsprogramme der App. Der Name der Konstante wird im Verlauf gespeichert. */
enum class Program(
    /** Kurzname für Auswahl und Verlauf. */
    val label: String,
    val title: String,
    val subtitle: String,
    val exercisesHeading: String,
    val exercises: List<Exercise>,
    val rounds: Int,
    val intro: List<String>,
) {
    STRENGTH(
        label = "Workoutplan",
        title = "WORKOUTPLAN",
        subtitle = "${WorkoutPlan.ROUNDS} DURCHGÄNGE · CA. 10 MINUTEN",
        exercisesHeading = "ÜBUNGEN PRO DURCHGANG",
        exercises = WorkoutPlan.exercises,
        rounds = WorkoutPlan.ROUNDS,
        intro = listOf(
            "Jede Übung ${WorkoutPlan.EXERCISE_SECONDS} Sekunden",
            "Nach allen ${WorkoutPlan.exercises.size} Übungen ${WorkoutPlan.REST_SECONDS} Sekunden Pause",
            "Insgesamt ${WorkoutPlan.ROUNDS} Durchgänge",
            "Saubere Technik hat immer Vorrang vor Tempo",
        ),
    ),
    MOBILITY(
        label = "Running-Mobilisation",
        title = "RUNNING-MOBILISATION",
        subtitle = "${MobilityPlan.exercises.size} MOVES · CA. 12 MINUTEN",
        exercisesHeading = "DIE ${MobilityPlan.exercises.size} MOVES",
        exercises = MobilityPlan.exercises,
        rounds = 1,
        intro = listOf(
            "Am besten vor einem lockeren Lauf",
            "Es geht nicht ums Aufwärmen, sondern um Koordination: Bewege dich bewusst und kontrolliert",
            "Feste Zeit pro Übung und Seite, dazwischen kurz Zeit zum Umpositionieren",
            "Du brauchst ein Widerstandsband und eine Stütze (Wand oder Stuhl)",
        ),
    );

    val timeline: Timeline by lazy {
        when (this) {
            STRENGTH -> Timeline.standard()
            MOBILITY -> Timeline.sequence(
                exercises = MobilityPlan.exercises,
                prepareSeconds = MobilityPlan.PREPARE_SECONDS,
                changeSeconds = MobilityPlan.CHANGE_SECONDS,
                sideChangeSeconds = MobilityPlan.SIDE_CHANGE_SECONDS,
            )
        }
    }

    companion object {
        fun fromName(name: String?): Program? = entries.firstOrNull { it.name == name }
    }
}
