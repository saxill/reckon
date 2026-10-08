package com.sahilchanna.reckon.model

import android.content.Context
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What a round leaves behind: personal bests, the Daily streak, the per-drill
 * profile, and the pace window the headline number is a median of.
 *
 * [Store] reads and writes a real `SharedPreferences` file, so these run under
 * Robolectric rather than a bare JVM — the save blob round-tripping through
 * Android's own storage is part of what is being checked.
 *
 * The SDK is pinned because Robolectric downloads the framework image for it;
 * nothing here depends on the platform version.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoreTest {

    private lateinit var context: Context
    private lateinit var store: Store

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        store = Store(context)
    }

    private fun attempt(
        op: Operation = Operation.ADD,
        seconds: Double = 1.0,
        correct: Boolean = true,
    ) = Attempt(op, seconds, correct)

    private fun result(
        mode: GameMode,
        correct: Int = 0,
        wrong: Int = 0,
        elapsed: Double = 0.0,
        ranOutOfLives: Boolean = false,
        solveTimes: List<Double> = emptyList(),
        log: List<Attempt> = emptyList(),
    ) = RoundResult(
        mode = mode,
        correct = correct,
        wrong = wrong,
        elapsed = elapsed,
        ranOutOfLives = ranOutOfLives,
        solveTimes = solveTimes,
        misses = emptyList(),
        log = log,
    )

    /** A round of pure arithmetic, so the per-drill profile has something to show. */
    private fun arithmetic(correct: Int, wrong: Int = 0, seconds: Double = 1.0): List<Attempt> =
        List(correct) { attempt(seconds = seconds, correct = true) } +
            List(wrong) { attempt(seconds = seconds, correct = false) }

    /** Replaces the saved blob on disk and reopens the store over it. */
    private fun seed(data: SaveData) {
        val json = Json { encodeDefaults = true }
        context.getSharedPreferences("reckon.save.v1", Context.MODE_PRIVATE)
            .edit()
            .putString("reckon.save.v1", json.encodeToString(SaveData.serializer(), data))
            .commit()
        store = Store(context)
    }

    @Test
    fun `a sprint is a personal best only when it beats the last one`() {
        val first = store.record(result(GameMode.SPRINT, correct = 10, log = arithmetic(10)))
        assertTrue(first.isPersonalBest)
        assertEquals(10, store.data.value.bestSprint)

        val worse = store.record(result(GameMode.SPRINT, correct = 8, log = arithmetic(8)))
        assertFalse(worse.isPersonalBest)
        assertEquals(10, store.data.value.bestSprint)

        val better = store.record(result(GameMode.SPRINT, correct = 12, log = arithmetic(12)))
        assertTrue(better.isPersonalBest)
        assertEquals(12, store.data.value.bestSprint)
    }

    @Test
    fun `a marathon is a personal best only if it was completed and is the fastest`() {
        // Running out of lives is not a time, so it can never be a best.
        val bust = store.record(
            result(GameMode.MARATHON, wrong = 3, elapsed = 20.0, ranOutOfLives = true),
        )
        assertFalse(bust.isPersonalBest)
        assertNull(store.data.value.bestMarathon)

        val first = store.record(result(GameMode.MARATHON, correct = 20, elapsed = 41.5))
        assertTrue(first.isPersonalBest)
        assertEquals(41.5, store.data.value.bestMarathon!!, 1e-9)

        val slower = store.record(result(GameMode.MARATHON, correct = 20, elapsed = 45.0))
        assertFalse(slower.isPersonalBest)
        assertEquals(41.5, store.data.value.bestMarathon!!, 1e-9)

        val faster = store.record(result(GameMode.MARATHON, correct = 20, elapsed = 39.0))
        assertTrue(faster.isPersonalBest)
        assertEquals(39.0, store.data.value.bestMarathon!!, 1e-9)
    }

    @Test
    fun `the first Daily of the day sets the streak and the score`() {
        store.record(result(GameMode.DAILY, correct = 12))

        val data = store.data.value
        assertEquals(1, data.dailyStreak)
        assertEquals(12, data.dailyScore)
        assertEquals(DayKey.today, data.dailyLastPlayed)
        assertTrue(data.playedDailyToday)
    }

    @Test
    fun `playing the Daily again the same day leaves the streak and score alone`() {
        store.record(result(GameMode.DAILY, correct = 12))
        store.record(result(GameMode.DAILY, correct = 15))

        assertEquals(1, store.data.value.dailyStreak)
        assertEquals(12, store.data.value.dailyScore)
    }

    @Test
    fun `a Daily played yesterday extends the streak and a missed day resets it`() {
        seed(
            SaveData(
                dailyStreak = 4,
                dailyDate = DayKey.yesterday,
                dailyScore = 9,
                dailyLastPlayed = DayKey.yesterday,
            ),
        )
        store.record(result(GameMode.DAILY, correct = 11))
        assertEquals(5, store.data.value.dailyStreak)
        assertEquals(11, store.data.value.dailyScore)
        assertEquals(DayKey.today, store.data.value.dailyLastPlayed)

        // Any older gap starts over at one, however long the run was.
        seed(SaveData(dailyStreak = 9, dailyDate = "2020-01-01", dailyLastPlayed = "2020-01-01"))
        store.record(result(GameMode.DAILY, correct = 11))
        assertEquals(1, store.data.value.dailyStreak)
    }

    @Test
    fun `the per-drill profile counts every attempt but times only the solves`() {
        store.record(
            result(
                GameMode.SPRINT,
                correct = 1,
                wrong = 1,
                solveTimes = listOf(2.0),
                log = listOf(
                    attempt(Operation.MUL, seconds = 2.0, correct = true),
                    attempt(Operation.MUL, seconds = 5.5, correct = false),
                ),
            ),
        )

        val stats = store.data.value.perOperation.getValue(Operation.MUL.rawValue)
        assertEquals(2, stats.attempts)
        assertEquals(1, stats.solved)
        assertEquals(2.0, stats.totalSeconds, 1e-9)
        assertEquals(2.0, stats.average!!, 1e-9)
        assertEquals(0.5, stats.accuracy!!, 1e-9)
    }

    @Test
    fun `pace keeps the last fourteen rounds, oldest first`() {
        repeat(16) { round ->
            store.record(
                result(
                    GameMode.SPRINT,
                    correct = 1,
                    solveTimes = listOf(round + 1.0),
                    log = arithmetic(1),
                ),
            )
        }

        val pace = store.data.value.pace
        assertEquals(14, pace.size)
        assertEquals(List(14) { (it + 3).toDouble() }, pace)
        assertEquals(9.5, store.data.value.medianPace!!, 1e-9)
    }

    @Test
    fun `a round with no solves contributes nothing to pace`() {
        store.record(result(GameMode.MARATHON, wrong = 3, ranOutOfLives = true))
        assertTrue(store.data.value.pace.isEmpty())
        assertNull(store.data.value.medianPace)
    }

    @Test
    fun `the save survives being reopened`() {
        store.record(result(GameMode.SPRINT, correct = 7, solveTimes = listOf(3.0), log = arithmetic(7)))
        store.toggle(Operation.DIV)
        store.setLevel(4)

        val reopened = Store(context)
        assertEquals(7, reopened.data.value.bestSprint)
        assertEquals(listOf(3.0), reopened.data.value.pace)
        assertEquals(4, reopened.data.value.level)
        assertTrue(reopened.data.value.enabledOperations.contains(Operation.DIV))
    }

    @Test
    fun `toggling can never leave zero drills enabled`() {
        // The default set is add, sub, mul.
        store.toggle(Operation.SUB)
        store.toggle(Operation.MUL)
        assertEquals(listOf(Operation.ADD), store.data.value.enabledOperations)

        // The last one standing refuses to switch off.
        store.toggle(Operation.ADD)
        assertEquals(listOf(Operation.ADD), store.data.value.enabledOperations)
    }

    @Test
    fun `the difficulty level is clamped to the supported range`() {
        store.setLevel(99)
        assertEquals(Difficulty.range.last, store.data.value.level)

        store.setLevel(0)
        assertEquals(Difficulty.range.first, store.data.value.level)
    }

    @Test
    fun `reset clears everything`() {
        store.record(result(GameMode.SPRINT, correct = 7, log = arithmetic(7)))
        store.setLevel(4)
        store.toggle(Operation.SUB)

        store.reset()

        assertEquals(SaveData(), store.data.value)
        assertEquals(SaveData(), Store(context).data.value)
    }

    @Test
    fun `a corrupt save falls back to a fresh one`() {
        context.getSharedPreferences("reckon.save.v1", Context.MODE_PRIVATE)
            .edit()
            .putString("reckon.save.v1", "{ not json at all")
            .commit()

        assertEquals(SaveData(), Store(context).data.value)
    }
}
