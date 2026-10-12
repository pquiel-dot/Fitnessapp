package de.pquiel.workout.model

/**
 * Ein Abschnitt einer Übung mit eigener Dauer, z. B. eine Körperseite.
 * [side] ist null, wenn die Übung nicht nach Seiten getrennt ist.
 */
data class Part(val seconds: Int, val side: String? = null)

/**
 * Eine Übung. Die Farbe ist ein ARGB-Wert, damit das Modell ohne Android auskommt.
 * [purpose] und [target] (Zweck und Wiederholungsziel) sowie [parts] nutzen nur Programme,
 * bei denen jede Übung eine eigene Dauer hat; leere [parts] bedeuten die Standarddauer des Plans.
 */
data class Exercise(
    val number: Int,
    val name: String,
    val germanName: String,
    val spokenName: String,
    val tips: List<String>,
    val colorArgb: Long,
    val purpose: String? = null,
    val target: String? = null,
    val parts: List<Part> = emptyList(),
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

/**
 * Running-Mobilisation: 6 Moves, um den Laufkörper zu mobilisieren (nach Runner's World 11/2026).
 * Jede Übung hat eine feste Zeit pro Seite; dazwischen kurze Wechselzeiten zum Umpositionieren.
 */
object MobilityPlan {
    const val PREPARE_SECONDS = 10
    const val CHANGE_SECONDS = 15
    const val SIDE_CHANGE_SECONDS = 10

    val exercises = listOf(
        Exercise(
            number = 1,
            name = "Banded Arm Circle",
            germanName = "Armkreisen mit Band",
            spokenName = "Armkreisen mit Band",
            purpose = "Mobilisiert die Schulterblätter für bessere Haltung und freieres Atmen.",
            target = "Insgesamt 20 Wiederholungen",
            tips = listOf(
                "Aufrecht hinstellen, Widerstandsband auf Taillenhöhe halten, Handflächen nach oben",
                "Griff so anpassen, dass Spannung auf dem Band ist",
                "Ellbogen strecken und das Band nach oben über den Kopf führen, bis es hinten wieder auf Taillenhöhe ist",
                "Weder ins Hohlkreuz fallen noch die Rippen aufklappen",
                "Zurück nach vorn und wiederholen",
            ),
            colorArgb = 0xFF0284C7,
            parts = listOf(Part(60)),
        ),
        Exercise(
            number = 2,
            name = "Twisted Warrior",
            germanName = "Gedrehter Krieger",
            spokenName = "Gedrehter Krieger",
            purpose = "Mobilisiert die Brustwirbelsäule für bessere Rotation.",
            target = "5 Drehungen pro Seite",
            tips = listOf(
                "Hoher Ausfallschritt: vorderes Bein gebeugt, hinteres Bein bleibt gestreckt",
                "Beide Hände innen neben den vorderen Fuß auf den Boden setzen",
                "Eine Hand anheben, Oberkörper aufdrehen und den Arm nach oben Richtung Himmel strecken",
                "Zurückdrehen, Hand absetzen – dann zur anderen Seite drehen und den anderen Arm nach oben nehmen",
                "Der ganze Oberkörper dreht sich – nicht nur Arme und Kopf",
            ),
            colorArgb = 0xFF8E2C6F,
            parts = listOf(Part(45, "Rechtes Bein vorn"), Part(45, "Linkes Bein vorn")),
        ),
        Exercise(
            number = 3,
            name = "Side-Lying Hip Circle",
            germanName = "Hüftkreisen in Seitenlage",
            spokenName = "Hüftkreisen in Seitenlage",
            purpose = "Verbessert Hüftmobilität und Rotationskontrolle.",
            target = "3 bis 5 Wiederholungen pro Seite",
            tips = listOf(
                "Auf die Seite legen, Knie leicht gebeugt, Arme nach vorn ausgestreckt, Hände verschränkt",
                "Oberes Knie Richtung Brust ziehen (Flexion)",
                "Hüfte öffnen: Knie und Unterschenkel Richtung Decke – Becken bleibt nach vorn, Hüften übereinander",
                "Hüfte nach innen rotieren, die Fußsohle dreht Richtung Decke",
                "Oberschenkel hinter die Körpermittellinie strecken und das Knie zurück zur Startposition führen",
                "Dann umgekehrt: Hüftstreckung, Hüfte öffnen (Knie höher als Fuß), Oberschenkel nach vorn zurück",
            ),
            colorArgb = 0xFFEA580C,
            parts = listOf(
                Part(60, "Linke Seitenlage – rechtes Bein"),
                Part(60, "Rechte Seitenlage – linkes Bein"),
            ),
        ),
        Exercise(
            number = 4,
            name = "Deep Squat Rock",
            germanName = "Gewicht kreisen in tiefer Hocke",
            spokenName = "Tiefe Hocke mit Gewichtsverlagerung",
            purpose = "Mobilisiert Sprunggelenke, Knie und Hüften in tiefer Beugung.",
            target = "10 Kreise pro Richtung",
            tips = listOf(
                "Tiefe Hocke, Füße etwas breiter als hüftbreit und ca. 45° nach außen gedreht",
                "Brust bleibt aufgerichtet, Arme nach vorn – oder an einer Stütze festhalten",
                "Gewicht langsam kreisen lassen: über einen Fuß, zu den Fußballen, über den anderen Fuß, zu den Fersen",
                "Fersen und Zehen dürfen mitkippen, während das Gewicht wandert",
            ),
            colorArgb = 0xFF0F9F8F,
            parts = listOf(Part(45, "Im Uhrzeigersinn"), Part(45, "Gegen den Uhrzeigersinn")),
        ),
        Exercise(
            number = 5,
            name = "Pigeon Hip Extension",
            germanName = "Hüftstreckung aus der Taube",
            spokenName = "Hüftstreckung aus der Taube",
            purpose = "Bewegt die Hüfte in Streckung, ohne unteren Rücken und Becken zu verändern.",
            target = "20 Wiederholungen pro Seite",
            tips = listOf(
                "Aus dem Kniestand ein Schienbein horizontal vor dir auf dem Boden ablegen",
                "Das andere Bein geht nach hinten: Zehen aufgestellt, Knie am Boden",
                "Hände stützen den Oberkörper",
                "Ohne das Becken zu bewegen: Gesäß anspannen und das hintere Knie vom Boden heben",
                "Gesäßspannung halten, während du das Knie wieder absetzt",
            ),
            colorArgb = 0xFF6D28D9,
            parts = listOf(Part(60, "Rechtes Bein hinten"), Part(60, "Linkes Bein hinten")),
        ),
        Exercise(
            number = 6,
            name = "Directional Leg Swing",
            germanName = "Beinschwingen in alle Richtungen",
            spokenName = "Beinschwingen in alle Richtungen",
            purpose = "Fördert multidirektionale Hüftmobilität – nicht nur vor und zurück.",
            target = "4 bis 8 Schwünge pro Richtung",
            tips = listOf(
                "Aufrecht hinstellen und an einer Stütze festhalten",
                "Seitlich schwingen: über den Körper hinweg und nach außen",
                "Dann diagonal schwingen – von 10 Uhr zu 4 Uhr",
                "Zum Schluss vor und zurück – von 12 Uhr zu 6 Uhr",
                "Nur aus der Hüfte bewegen: kein Hohlkreuz, kein Rundrücken, Becken nicht kippen",
            ),
            colorArgb = 0xFFDB2777,
            parts = listOf(Part(60, "Rechtes Bein"), Part(60, "Linkes Bein")),
        ),
    )
}
