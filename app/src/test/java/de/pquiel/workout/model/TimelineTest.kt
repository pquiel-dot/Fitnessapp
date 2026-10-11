package de.pquiel.workout.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineTest {
    private val timeline = Timeline.standard()
    private val prep = WorkoutPlan.PREPARE_SECONDS * 1000L

    @Test
    fun workoutTakesTenMinutes() {
        assertEquals(10 * 60 * 1000L, timeline.workoutMs)
        assertEquals(10 * 60 * 1000L + prep, timeline.totalMs)
        assertEquals("10:00", formatClock(timeline.workoutMs))
    }

    @Test
    fun hasPrepareTwelveExercisesAndTwoRests() {
        val segs = timeline.segments
        assertEquals(15, segs.size)
        assertTrue(segs.first() is Prepare)
        assertEquals(12, segs.count { it is Work })
        assertEquals(listOf(0, 1), segs.filterIsInstance<Rest>().map { it.afterRound })
        assertTrue(segs.last() is Work)
        // Reihenfolge: 4 Übungen, Pause, 4 Übungen, Pause, 4 Übungen
        assertTrue(segs[5] is Rest)
        assertTrue(segs[10] is Rest)
    }

    @Test
    fun positionAtStartIsPrepare() {
        val pos = timeline.positionAt(0)
        assertEquals(0, pos.index)
        assertTrue(pos.segment is Prepare)
        assertEquals(10, pos.remainingSeconds)
        assertFalse(pos.finished)
    }

    @Test
    fun firstExerciseStartsAfterPrepare() {
        val pos = timeline.positionAt(prep)
        val work = pos.segment as Work
        assertEquals(0, work.round)
        assertEquals(0, work.indexInRound)
        assertEquals(45, pos.remainingSeconds)
        assertEquals(44, timeline.positionAt(prep + 1000).remainingSeconds)
        assertEquals(45, timeline.positionAt(prep + 999).remainingSeconds)
    }

    @Test
    fun restFollowsFirstRound() {
        val pos = timeline.positionAt(prep + 180_000)
        assertTrue(pos.segment is Rest)
        assertEquals(30, pos.remainingSeconds)
        val nextRound = timeline.positionAt(prep + 210_000).segment as Work
        assertEquals(1, nextRound.round)
        assertEquals(0, nextRound.indexInRound)
    }

    @Test
    fun finishedAtEnd() {
        assertFalse(timeline.positionAt(timeline.totalMs - 1).finished)
        assertTrue(timeline.positionAt(timeline.totalMs).finished)
        assertEquals(0L, timeline.workoutRemainingMs(timeline.totalMs))
        assertEquals(1f, timeline.workoutProgress(timeline.totalMs), 0.0001f)
    }

    @Test
    fun remainingIgnoresPrepare() {
        assertEquals(timeline.workoutMs, timeline.workoutRemainingMs(0))
        assertEquals(timeline.workoutMs, timeline.workoutRemainingMs(prep))
        assertEquals(timeline.workoutMs - 1000, timeline.workoutRemainingMs(prep + 1000))
    }

    @Test
    fun startOffsetsMatchPositions() {
        for (i in timeline.segments.indices) {
            assertEquals(i, timeline.positionAt(timeline.startOf(i)).index)
        }
    }

    @Test
    fun nextWorkSkipsRest() {
        val next = timeline.nextWork(5)!!
        assertEquals(1, next.round)
        assertNull(timeline.nextWork(timeline.segments.size))
    }

    @Test
    fun formatClockRoundsUp() {
        assertEquals("0:45", formatClock(45_000))
        assertEquals("0:45", formatClock(44_001))
        assertEquals("1:00", formatClock(59_500))
        assertEquals("0:00", formatClock(0))
    }
}
