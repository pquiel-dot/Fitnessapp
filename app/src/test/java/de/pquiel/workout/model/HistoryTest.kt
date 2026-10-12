package de.pquiel.workout.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryTest {
    private val full = WorkoutRecord(
        startedAt = 1_000_000,
        finishedAt = 1_610_000,
        trainedMs = 600_000,
        exerciseCounts = mapOf(1 to 3, 2 to 3, 3 to 3, 4 to 3),
        plannedExercises = 12,
    )
    private val short = WorkoutRecord(
        startedAt = 2_000_000,
        finishedAt = 2_300_000,
        trainedMs = 250_000,
        exerciseCounts = mapOf(1 to 2, 2 to 1),
        plannedExercises = 12,
    )
    private val latest = WorkoutRecord(
        startedAt = 3_000_000,
        finishedAt = 3_500_000,
        trainedMs = 450_000,
        exerciseCounts = mapOf(1 to 3, 2 to 2, 3 to 2, 4 to 2),
        plannedExercises = 12,
    )
    private val all = listOf(short, full, latest)

    @Test
    fun encodeDecodeRoundTrip() {
        assertEquals(full, WorkoutRecord.decode(full.encode()))
        assertEquals(all, WorkoutRecord.decodeAll(WorkoutRecord.encodeAll(all)))
        assertEquals(emptyList<WorkoutRecord>(), WorkoutRecord.decodeAll(WorkoutRecord.encodeAll(emptyList())))
    }

    @Test
    fun invalidLinesAreSkipped() {
        assertNull(WorkoutRecord.decode("kaputt"))
        assertNull(WorkoutRecord.decode("1;2;3;12;1:x"))
        val text = "müll\n" + full.encode() + "\n\n1;2\n"
        assertEquals(listOf(full), WorkoutRecord.decodeAll(text))
    }

    @Test
    fun completedExercisesSumsCounts() {
        assertEquals(12, full.completedExercises)
        assertEquals(3, short.completedExercises)
    }

    @Test
    fun sortByDate() {
        assertEquals(listOf(latest, short, full), HistorySort.NEWEST.sort(all))
        assertEquals(listOf(full, short, latest), HistorySort.OLDEST.sort(all))
    }

    @Test
    fun sortByDuration() {
        assertEquals(listOf(full, latest, short), HistorySort.LONGEST.sort(all))
        assertEquals(listOf(short, latest, full), HistorySort.SHORTEST.sort(all))
    }

    @Test
    fun sortByExercisesWithNewestAsTieBreaker() {
        val sameAsFull = full.copy(startedAt = 4_000_000, finishedAt = 4_610_000)
        val list = all + sameAsFull
        assertEquals(listOf(sameAsFull, full, latest, short), HistorySort.MOST_EXERCISES.sort(list))
        assertEquals(listOf(short, latest, sameAsFull, full), HistorySort.FEWEST_EXERCISES.sort(list))
    }

    @Test
    fun summaryAddsUp() {
        val summary = HistorySummary.of(all)
        assertEquals(3, summary.workouts)
        assertEquals(1_300_000L, summary.trainedMs)
        assertEquals(24, summary.completedExercises)
        assertEquals(mapOf(Program.STRENGTH to mapOf(1 to 8, 2 to 6, 3 to 5, 4 to 5)), summary.exerciseCounts)
    }

    @Test
    fun programIsStoredAndOldLinesAreStrength() {
        val mobility = full.copy(program = Program.MOBILITY, exerciseCounts = mapOf(1 to 1, 2 to 1), plannedExercises = 6)
        assertEquals(mobility, WorkoutRecord.decode(mobility.encode()))
        assertEquals(full, WorkoutRecord.decode("1000000;1610000;600000;12;1:3,2:3,3:3,4:3"))
        assertNull(WorkoutRecord.decode("1;2;3;6;1:1;UNBEKANNT"))
    }

    @Test
    fun summaryKeepsProgramsApart() {
        val mobility = latest.copy(program = Program.MOBILITY, exerciseCounts = mapOf(1 to 1, 3 to 1), plannedExercises = 6)
        val summary = HistorySummary.of(listOf(full, mobility))
        assertEquals(mapOf(1 to 3, 2 to 3, 3 to 3, 4 to 3), summary.exerciseCounts[Program.STRENGTH])
        assertEquals(mapOf(1 to 1, 3 to 1), summary.exerciseCounts[Program.MOBILITY])
        assertEquals(14, summary.completedExercises)
    }

    @Test
    fun formatDurationRoundsToNearestSecond() {
        assertEquals("10:00", formatDuration(600_080))
        assertEquals("0:45", formatDuration(44_600))
        assertEquals("1:05:00", formatDuration(3_900_000))
        assertEquals("0:00", formatDuration(-5))
    }
}
