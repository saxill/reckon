package com.sahilchanna.reckon.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Daily set is seeded from the date so that every device gets the same 15
 * problems. That promise only holds on Android if the port reproduces Swift's
 * generator exactly — the mulberry32 stream *and* the uniform-int method drawn
 * from it.
 *
 * Every expected value below is output from the real iOS sources, produced by
 * `android/parity/` (`swiftc` over `Operation.swift` + `Problem.swift` +
 * `main.swift`). If this test fails, the two platforms have silently started
 * disagreeing about what "today's Daily" is. Regenerate the expectations by
 * rerunning that harness; do not edit them to match the code.
 */
class DailyParityTest {

    private val dateKey = "2026-10-08"

    @Test
    fun `raw stream matches Swift bit for bit`() {
        val expected = listOf(
            9667831875569363997uL,
            854222711787640129uL,
            5948699456743933241uL,
            11392416714758338009uL,
            10310308808634140571uL,
            3320202288711005920uL,
        )
        val rng = Mulberry32.forDate(dateKey)
        assertEquals(expected, List(expected.size) { rng.next() })
    }

    /**
     * The load-bearing one. Swift draws these via Lemire multiply-shift; a naive
     * `n % count` port agrees on most of them and then quietly diverges. 2...9 is
     * the case that catches it.
     */
    @Test
    fun `uniform draws over the factory's bounds match Swift`() {
        val cases = listOf(
            2..20 to listOf(11, 2, 8, 13),
            10..99 to listOf(57, 14, 39, 65),
            2..9 to listOf(6, 2, 4, 6),
            3..19 to listOf(11, 3, 8, 13),
            11..49 to listOf(31, 12, 23, 35),
            12..99 to listOf(58, 16, 40, 66),
            1000..9999 to listOf(5716, 1416, 3902, 6558),
        )
        for ((range, expected) in cases) {
            val rng = Mulberry32.forDate(dateKey)
            val got = List(expected.size) { randomIn(range, rng) }
            assertEquals("bounds $range", expected, got)
        }
    }

    @Test
    fun `a whole Daily set matches the iOS output`() {
        val expected = listOf(
            "3 × 5" to 15,
            "120 − 52" to 68,
            "127 + 154" to 281,
            "72 ÷ 9" to 8,
            "110 + 63" to 173,
            "175 − 103" to 72,
            "3 × 8" to 24,
            "29 + 128" to 157,
            "114 ÷ 6" to 19,
            "188 − 22" to 166,
            "121 + 87" to 208,
            "11 × 8" to 88,
            "76 ÷ 4" to 19,
            "46 + 112" to 158,
            "59 − 36" to 23,
        )

        val rng = Mulberry32.forDate(dateKey)
        var last: Operation? = null
        val got = (0 until 15).map {
            val p = ProblemFactory.next(Operation.entries.toList(), level = 3, avoiding = last, rng = rng)
            last = p.operation
            p.question to p.answer
        }

        assertEquals(expected, got)
    }

    /**
     * A division problem must divide cleanly and a subtraction must not go
     * negative — the two invariants the keypad's digit-only design depends on.
     */
    @Test
    fun `generated problems stay well formed`() {
        val rng = Mulberry32(12345u)
        var last: Operation? = null
        repeat(2_000) {
            val p = ProblemFactory.next(Operation.entries.toList(), level = 3, avoiding = last, rng = rng)
            last = p.operation

            assert(p.answer >= 0) { "negative answer: ${p.question}" }

            if (p.operation == Operation.DIV) {
                // "dividend ÷ divisor" must divide exactly, or the answer isn't an integer.
                val (dividend, divisor) = p.question.split(" ÷ ").map(String::toInt)
                assert(divisor != 0) { "zero divisor: ${p.question}" }
                assert(dividend % divisor == 0) { "inexact division: ${p.question}" }
                assert(dividend / divisor == p.answer) { "wrong quotient: ${p.question}" }
            }
        }
    }

    /** The generator is a pure function of the date key. */
    @Test
    fun `the seed depends only on the date`() {
        assertEquals(Mulberry32.fnv1a("2026-10-08"), Mulberry32.fnv1a("2026-10-08"))
        assert(Mulberry32.fnv1a("2026-10-08") != Mulberry32.fnv1a("2026-10-09"))
    }
}
