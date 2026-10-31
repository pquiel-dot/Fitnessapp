package de.pquiel.workout.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFileTest {
    private val a = WorkoutRecord(1_000, 2_000, 900, mapOf(1 to 3, 2 to 3), 12)
    private val b = WorkoutRecord(3_000, 4_000, 800, mapOf(1 to 1), 6, Program.MOBILITY)
    private val c = WorkoutRecord(5_000, 6_000, 700, mapOf(4 to 2), 12)

    @Test
    fun roundTripWithHeader() {
        val text = HistoryFile.encode(listOf(c, a, b))
        assertTrue(text.startsWith(HistoryFile.HEADER))
        assertEquals(listOf(a, b, c), HistoryFile.decode(text))
    }

    @Test
    fun emptyFilesAreValid() {
        assertEquals(emptyList<WorkoutRecord>(), HistoryFile.decode(""))
        assertEquals(emptyList<WorkoutRecord>(), HistoryFile.decode(HistoryFile.encode(emptyList())))
    }

    @Test
    fun oldFilesWithoutHeaderAreRead() {
        assertEquals(listOf(a, b), HistoryFile.decode(WorkoutRecord.encodeAll(listOf(a, b))))
    }

    @Test
    fun foreignFilesAreRejected() {
        assertNull(HistoryFile.decode("Einkaufsliste\nMilch\nBrot\n"))
    }

    @Test
    fun mergeRemovesDuplicatesAndSorts() {
        assertEquals(listOf(a, b, c), HistoryFile.merge(listOf(c, a), listOf(a, b)))
        assertEquals(listOf(a), HistoryFile.merge(emptyList(), listOf(a)))
    }
}
