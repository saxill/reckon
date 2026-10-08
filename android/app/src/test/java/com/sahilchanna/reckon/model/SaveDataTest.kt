package com.sahilchanna.reckon.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The numbers the Home screen derives from the save blob. None of this touches
 * Android, so it is plain JUnit — the store tests next door cover the writing.
 */
class SaveDataTest {

    @Test
    fun `median pace is the middle of an odd count and the mean of the middle pair otherwise`() {
        assertNull(SaveData().medianPace)

        assertEquals(3.0, SaveData(pace = listOf(3.0)).medianPace!!, 1e-9)

        // Unsorted on purpose: the saved window is chronological, not ordered.
        assertEquals(4.0, SaveData(pace = listOf(9.0, 4.0, 1.0)).medianPace!!, 1e-9)
        assertEquals(6.5, SaveData(pace = listOf(9.0, 4.0, 1.0, 12.0)).medianPace!!, 1e-9)
    }

    @Test
    fun `a drill needs three solves before it is worth reporting on`() {
        val thin = SaveData(perOperation = mapOf("mul" to OpStats(attempts = 2, solved = 2, totalSeconds = 4.0)))
        assertNull(thin.statsFor(Operation.MUL))

        val enough = SaveData(perOperation = mapOf("mul" to OpStats(attempts = 4, solved = 3, totalSeconds = 6.0)))
        assertEquals(2.0, enough.statsFor(Operation.MUL)!!.average!!, 1e-9)
        assertEquals(0.75, enough.statsFor(Operation.MUL)!!.accuracy!!, 1e-9)
    }

    @Test
    fun `the weakest drill is the slowest one with enough evidence`() {
        // Nothing has three solves yet, so there is no weakest drill to name.
        assertNull(
            SaveData(perOperation = mapOf("mul" to OpStats(attempts = 1, solved = 1, totalSeconds = 99.0)))
                .weakestOperation,
        )

        val data = SaveData(
            perOperation = mapOf(
                "add" to OpStats(attempts = 3, solved = 3, totalSeconds = 3.0),
                "sub" to OpStats(attempts = 3, solved = 3, totalSeconds = 12.0),
                // Faster than everything, but only one solve in.
                "div" to OpStats(attempts = 1, solved = 1, totalSeconds = 99.0),
            ),
        )

        assertEquals(Operation.SUB, data.weakestOperation!!.first)
        assertEquals(4.0, data.weakestOperation!!.second, 1e-9)
    }

    @Test
    fun `an empty per-drill tally has no average and no accuracy`() {
        val empty = OpStats()
        assertNull(empty.average)
        assertNull(empty.accuracy)
    }

    @Test
    fun `the Daily is only played today when the saved date is today`() {
        assertFalse(SaveData().playedDailyToday)
        assertTrue(SaveData(dailyDate = DayKey.today).playedDailyToday)
        assertFalse(SaveData(dailyDate = "2020-01-01").playedDailyToday)
    }

    @Test
    fun `a drill name a future build wrote is ignored rather than crashing`() {
        val data = SaveData(operations = listOf("add", "somethingnew", "sub"))
        assertEquals(listOf(Operation.ADD, Operation.SUB), data.enabledOperations)
    }
}
