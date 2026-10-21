package de.pquiel.workout.strava

import de.pquiel.workout.model.Program
import de.pquiel.workout.model.WorkoutRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StravaActivityTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val start = LocalDateTime.of(2026, 10, 21, 7, 30, 5).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun strengthWorkoutBecomesWeightTraining() {
        val record = WorkoutRecord(
            startedAt = start,
            finishedAt = start + 620_000,
            trainedMs = 599_600,
            exerciseCounts = mapOf(1 to 3, 2 to 3, 3 to 2, 4 to 3),
            plannedExercises = 12,
        )
        val activity = StravaActivity.of(record, zone)

        assertEquals("Workoutplan", activity.name)
        assertEquals("WeightTraining", activity.sportType)
        assertEquals("2026-10-21T07:30:05", activity.startDateLocal)
        assertEquals(600, activity.elapsedSeconds)
        assertEquals(
            listOf(
                "11 von 12 Übungen absolviert",
                "",
                "1. Bodyweight Squats (Kniebeugen): 3×",
                "2. Push-Ups (Liegestütze): 3×",
                "3. Reverse Lunges (Rückwärts-Ausfallschritte): 2×",
                "4. Plank to Shoulder Tap (Liegestützposition mit Schultertippen): 3×",
                "",
                "Aufgezeichnet mit Workout Begleiter",
            ).joinToString("\n"),
            activity.description,
        )
        assertEquals(
            listOf("name", "sport_type", "start_date_local", "elapsed_time", "description"),
            activity.formParams().keys.toList(),
        )
        assertEquals("600", activity.formParams()["elapsed_time"])
    }

    @Test
    fun mobilityMarksSkippedMoves() {
        val record = WorkoutRecord(
            startedAt = start,
            finishedAt = start + 700_000,
            trainedMs = 650_000,
            exerciseCounts = mapOf(1 to 1, 2 to 1, 3 to 1, 4 to 1, 6 to 1),
            plannedExercises = 6,
            program = Program.MOBILITY,
        )
        val activity = StravaActivity.of(record, zone)

        assertEquals("Running-Mobilisation", activity.name)
        assertEquals("Workout", activity.sportType)
        assertTrue(activity.description.contains("✓ 1. Banded Arm Circle (Armkreisen mit Band)"))
        assertTrue(activity.description.contains("– 5. Pigeon Hip Extension (Hüftstreckung aus der Taube)"))
        assertTrue(activity.description.startsWith("5 von 6 Übungen absolviert"))
    }

    @Test
    fun elapsedTimeIsAtLeastOneSecond() {
        val record = WorkoutRecord(start, start, trainedMs = 0, exerciseCounts = mapOf(1 to 1), plannedExercises = 12)
        assertEquals(1, StravaActivity.of(record, zone).elapsedSeconds)
    }

    @Test
    fun authorizeUrlIsEncoded() {
        assertEquals(
            "https://www.strava.com/oauth/mobile/authorize?client_id=12345" +
                "&redirect_uri=workoutbegleiter%3A%2F%2Flocalhost%2Fstrava&response_type=code" +
                "&approval_prompt=auto&scope=read%2Cactivity%3Awrite&state=abc",
            StravaAuth.authorizeUrl("12345", "abc"),
        )
    }

    @Test
    fun writeScopeIsRequired() {
        assertTrue(StravaAuth.canWrite("read,activity:write"))
        assertFalse(StravaAuth.canWrite("read"))
        assertFalse(StravaAuth.canWrite("read,activity:write_all"))
        assertFalse(StravaAuth.canWrite(null))
    }

    @Test
    fun formEncodingEscapesSpecialCharacters() {
        assertEquals(
            "name=Running-Mobilisation&description=%E2%9C%93+1.+A%26B",
            StravaAuth.formEncode(linkedMapOf("name" to "Running-Mobilisation", "description" to "✓ 1. A&B")),
        )
    }
}
