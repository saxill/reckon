package com.sahilchanna.reckon.model

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The engine's rules: what counts, what a miss costs, and when a round is over.
 *
 * Nothing here waits on wall-clock time. The engine takes its clock as a
 * `() -> Double` and runs its post-answer pauses on the scope it is handed, so a
 * [TestScope] plus a hand-advanced [Clock] pins every one of these exactly rather
 * than making them merely likely.
 */
class EngineTest {

    /** Stand-in for wall time; the engine only ever reads it, so the test owns it. */
    private class Clock(var value: Double = 0.0)

    private fun newEngine(
        scope: TestScope,
        mode: GameMode,
        ops: List<Operation> = listOf(Operation.ADD),
        level: Int = 2,
        clock: Clock,
        dateKey: String = "2026-10-08",
    ) = GameEngine(
        mode = mode,
        operations = ops,
        level = level,
        scope = scope,
        dateKey = dateKey,
        now = { clock.value },
    )

    /** The keypad auto-submits once the typed answer is as long as the answer. */
    private fun GameEngine.typeAll(digits: String) {
        for (character in digits) type(character.toString())
    }

    private fun GameEngine.answerCorrectly() = typeAll(problem.answer.toString())

    /**
     * A same-length answer that cannot be right: every digit is replaced, and
     * never with a zero — a leading zero would be swallowed by the engine's
     * "don't keep a lone 0" rule and the input would never reach full length.
     */
    private fun GameEngine.answerWrongly() {
        val correct = problem.answer.toString()
        val wrong = correct.map { if (it == '9') '8' else '9' }.joinToString("")
        assertNotEquals("the wrong answer must actually differ", correct, wrong)
        typeAll(wrong)
    }

    @Test
    fun `a correct answer scores and auto-submits at the answer's length`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.SPRINT, clock = clock)
        engine.start {}

        engine.answerCorrectly()

        assertEquals(1, engine.correct)
        assertEquals(0, engine.wrong)
        assertEquals(1, engine.answered)
        assertEquals(GameEngine.Feedback.CORRECT, engine.feedback)

        advanceUntilIdle()

        // The next problem is live again: input cleared, feedback reset.
        assertEquals("", engine.typed)
        assertEquals(GameEngine.Feedback.IDLE, engine.feedback)
    }

    @Test
    fun `sprint charges two seconds for a miss and keeps no lives`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.SPRINT, clock = clock)
        engine.start {}

        clock.value = 3.0
        engine.answerWrongly()

        assertEquals(1, engine.wrong)
        assertEquals(0, engine.correct)
        // Sprint has no lives, so a miss must not move the counter.
        assertEquals(0, engine.livesLeft)

        engine.tick()
        assertEquals(5.0, engine.elapsed, 1e-9)

        // Let the miss's pause finish so nothing is left running.
        advanceUntilIdle()
    }

    @Test
    fun `sprint finishes the instant the clock runs out`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.SPRINT, clock = clock)
        val results = mutableListOf<RoundResult>()
        engine.start { results += it }

        clock.value = 59.9
        engine.tick()
        assertFalse(engine.isFinished)
        assertTrue(results.isEmpty())

        clock.value = 60.1
        engine.tick()

        assertTrue(engine.isFinished)
        assertEquals(1, results.size)
        assertEquals(60.1, results.single().elapsed, 1e-9)
    }

    @Test
    fun `marathon loses a life per miss and ends at the third`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.MARATHON, clock = clock)
        val results = mutableListOf<RoundResult>()
        engine.start { results += it }

        engine.answerWrongly()
        advanceUntilIdle()
        assertEquals(2, engine.livesLeft)
        assertFalse(engine.isFinished)

        engine.answerWrongly()
        advanceUntilIdle()
        assertEquals(1, engine.livesLeft)
        assertFalse(engine.isFinished)

        engine.answerWrongly()
        advanceUntilIdle()

        assertEquals(0, engine.livesLeft)
        assertTrue(engine.isFinished)
        assertEquals(3, results.single().wrong)
        assertTrue(results.single().ranOutOfLives)
        // A miss costs no time outside sprint.
        assertEquals(0.0, results.single().elapsed, 1e-9)
    }

    @Test
    fun `marathon finishes after twenty problems without running out of lives`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.MARATHON, clock = clock)
        val results = mutableListOf<RoundResult>()
        engine.start { results += it }

        repeat(20) {
            engine.answerCorrectly()
            advanceUntilIdle()
        }

        assertTrue(engine.isFinished)
        val result = results.single()
        assertEquals(20, result.correct)
        assertEquals(20, result.seen)
        assertEquals(3, engine.livesLeft)
        assertFalse(result.ranOutOfLives)
    }

    @Test
    fun `daily is the same fifteen problems for the same date and a different set otherwise`() = runTest {
        val scope = this
        val clock = Clock()

        fun dailyQuestions(dateKey: String): List<String> {
            val engine = newEngine(scope, GameMode.DAILY, clock = clock, dateKey = dateKey)
            engine.start {}
            return buildList {
                repeat(15) {
                    add(engine.problem.question)
                    // Correct answers only: an early finish would cut the set short.
                    engine.answerCorrectly()
                    scope.advanceUntilIdle()
                }
            }
        }

        val today = dailyQuestions("2026-10-08")
        assertEquals(15, today.size)
        assertEquals(today, dailyQuestions("2026-10-08"))
        assertNotEquals(today, dailyQuestions("2026-10-09"))
    }

    @Test
    fun `daily finishes after fifteen problems`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.DAILY, clock = clock)
        val results = mutableListOf<RoundResult>()
        engine.start { results += it }

        repeat(15) {
            engine.answerCorrectly()
            advanceUntilIdle()
        }

        assertTrue(engine.isFinished)
        assertEquals(15, results.single().correct)
        assertEquals(15, results.single().seen)
    }

    @Test
    fun `backspace and clear only touch what is typed`() = runTest {
        val clock = Clock()
        // Level 5 addition always answers in four digits or more, so three digits
        // can be typed without tripping the auto-submit.
        val engine = newEngine(this, GameMode.SPRINT, level = 5, clock = clock)
        engine.start {}
        assertTrue("level 5 answers must be long enough for this", engine.problem.digits >= 4)

        engine.typeAll("123")
        assertEquals("123", engine.typed)

        engine.backspace()
        assertEquals("12", engine.typed)

        engine.clear()
        assertEquals("", engine.typed)

        // Answering is untouched by either.
        assertEquals(0, engine.answered)
    }

    @Test
    fun `commit submits a partial answer and refuses an empty one`() = runTest {
        val clock = Clock()
        val engine = newEngine(this, GameMode.SPRINT, clock = clock)
        engine.start {}

        engine.commit()
        assertEquals("nothing typed, nothing submitted", 0, engine.answered)

        // Return submits whatever is there rather than waiting for auto-submit.
        engine.typeAll("9")
        engine.commit()
        assertEquals(1, engine.answered)

        // Let the miss's pause finish so nothing is left running.
        advanceUntilIdle()
    }

    @Test
    fun `the clock counts down in sprint and up in marathon`() = runTest {
        val clock = Clock()

        val sprint = newEngine(this, GameMode.SPRINT, clock = clock)
        sprint.start {}
        clock.value = 12.0
        sprint.tick()
        assertEquals("0:48", sprint.clockText)
        assertEquals(48.0, sprint.remaining!!, 1e-9)

        clock.value = 0.0
        val marathon = newEngine(this, GameMode.MARATHON, clock = clock)
        marathon.start {}
        clock.value = 72.0
        marathon.tick()
        assertEquals("1:12", marathon.clockText)
        assertEquals(null, marathon.remaining)
    }

    @Test
    fun `sprint progress drains with the clock and marathon progress fills with answers`() = runTest {
        val clock = Clock()

        val sprint = newEngine(this, GameMode.SPRINT, clock = clock)
        sprint.start {}
        clock.value = 15.0
        sprint.tick()
        assertEquals(0.75, sprint.progress, 1e-9)

        val marathon = newEngine(this, GameMode.MARATHON, clock = clock)
        marathon.start {}
        marathon.answerCorrectly()
        advanceUntilIdle()
        assertEquals(0.05, marathon.progress, 1e-9)
    }
}
