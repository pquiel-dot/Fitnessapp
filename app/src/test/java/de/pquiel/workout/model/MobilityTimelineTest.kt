package de.pquiel.workout.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilityTimelineTest {
    private val timeline = Program.MOBILITY.timeline
    private val works = timeline.segments.filterIsInstance<Work>()

    @Test
    fun allSixMovesInOrder() {
        assertEquals(6, MobilityPlan.exercises.size)
        assertEquals((1..6).toList(), works.map { it.exercise.number }.distinct())
        assertEquals(6, timeline.plannedExercises)
        assertTrue(timeline.segments.first() is Prepare)
        assertTrue(timeline.segments.last() is Work)
    }

    @Test
    fun sidesFollowEachOtherWithChangeInBetween() {
        // Armkreisen hat eine Seite, die übrigen fünf Übungen je zwei.
        assertEquals(11, works.size)
        val changes = timeline.segments.filterIsInstance<Change>()
        assertEquals(10, changes.size)
        assertEquals(5, changes.count { it.next.part > 0 })
        assertEquals(MobilityPlan.SIDE_CHANGE_SECONDS * 1000L, changes.first { it.next.part > 0 }.durationMs)
        assertEquals(MobilityPlan.CHANGE_SECONDS * 1000L, changes.first { it.next.part == 0 }.durationMs)
        // Jede Wechselzeit kündigt genau die folgende Übung an.
        timeline.segments.forEachIndexed { i, s ->
            if (s is Change) assertEquals(timeline.segments[i + 1], s.next)
        }
        val warrior = works.filter { it.exercise.number == 2 }
        assertEquals(listOf("Rechtes Bein vorn", "Linkes Bein vorn"), warrior.map { it.side })
        assertEquals(listOf(false, true), warrior.map { it.isLastPart })
    }

    @Test
    fun takesAboutTwelveMinutes() {
        assertEquals(600_000L, works.sumOf { it.durationMs })
        assertEquals(725_000L, timeline.workoutMs)
        assertEquals("12:05", formatClock(timeline.workoutMs))
    }

    @Test
    fun exerciseCountsOnlyWhenAllSidesAreDone() {
        val index = { nr: Int, part: Int ->
            timeline.segments.indexOfFirst { it is Work && it.exercise.number == nr && it.part == part }
        }
        val done = setOf(index(1, 0), index(2, 0), index(2, 1), index(3, 0))
        assertEquals(mapOf(1 to 1, 2 to 1), timeline.completedExercises(done))
        assertEquals(emptyMap<Int, Int>(), timeline.completedExercises(emptySet()))
    }

    @Test
    fun strengthCountsAreUnchanged() {
        val strength = Program.STRENGTH.timeline
        assertEquals(12, strength.plannedExercises)
        val all = strength.segments.indices.filter { strength.segments[it] is Work }.toSet()
        assertEquals(mapOf(1 to 3, 2 to 3, 3 to 3, 4 to 3), strength.completedExercises(all))
    }
}
