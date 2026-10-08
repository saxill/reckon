package com.sahilchanna.reckon.model

data class Problem(val operation: Operation, val question: String, val answer: Int) {
    /** How many digits the answer has — the keypad submits itself at this count. */
    val digits: Int get() = answer.toString().length
}

/**
 * mulberry32 — small, fast, and seedable, so the Daily set is identical on every
 * device for a given date. A bit-for-bit port of the iOS `SeededRandom`; the
 * widget and the app share a history, and the Daily must agree across platforms.
 *
 * Kotlin's `UInt` wraps on overflow the way Swift's `&+` / `&*` do, so the
 * arithmetic below is a direct transcription.
 */
class Mulberry32(private var state: UInt) {

    private fun next32(): UInt {
        state += 0x6D2B79F5u
        var t = state
        t = (t xor (t shr 15)) * (1u or t)
        t = t xor (t + ((t xor (t shr 7)) * (61u or t)))
        return t xor (t shr 14)
    }

    fun next(): ULong = (next32().toULong() shl 32) or next32().toULong()

    companion object {
        /**
         * Seeded from a date key, so the Daily is the same set on every device.
         *
         * A factory rather than a second constructor: hashing the key is a step
         * worth naming at the call site, and a `String` overload beside the
         * `UInt` one would hide which of the two a given seed came from.
         */
        fun forDate(dateKey: String): Mulberry32 = Mulberry32(fnv1a(dateKey))

        /** FNV-1a over the date key's UTF-8 bytes. */
        fun fnv1a(dateKey: String): UInt {
            var h: UInt = 2_166_136_261u
            for (b in dateKey.encodeToByteArray()) {
                h = h xor b.toUByte().toUInt()
                h *= 16_777_619u
            }
            return h
        }
    }
}

/**
 * The high 64 bits of the unsigned 128-bit product `x * y`.
 *
 * `Math.multiplyHigh` would be the one-liner, but it only exists from Android
 * API 33 and this app supports 26 — so this does the schoolbook split by hand
 * instead. Every partial product fits in 64 bits, and the final wrap is exactly
 * the truncation we want.
 */
internal fun high64(x: ULong, y: ULong): ULong {
    val x0 = x and 0xFFFFFFFFuL
    val x1 = x shr 32
    val y0 = y and 0xFFFFFFFFuL
    val y1 = y shr 32

    val p00 = x0 * y0
    val p01 = x0 * y1
    val p10 = x1 * y0
    val p11 = x1 * y1

    val mid = (p00 shr 32) + (p01 and 0xFFFFFFFFuL) + (p10 and 0xFFFFFFFFuL)
    return p11 + (p01 shr 32) + (p10 shr 32) + (mid shr 32)
}

/**
 * Swift's `Int.random(in:using:)`, reproduced exactly.
 *
 * Swift does *not* take `n % count`. It uses Lemire's multiply-shift: the high
 * half of `n * count`. The difference is invisible for small counts most of the
 * time and then diverges — which for a date-seeded Daily would mean the iPhone
 * and the Android phone disagreeing about "the same set for everyone".
 *
 * Verified against the real Swift implementation in `android/parity/` and pinned
 * by `DailyParityTest`.
 */
fun randomIn(range: IntRange, rng: Mulberry32): Int {
    val count = (range.last - range.first + 1).toULong()
    // Swift special-cases a bound of the full width; nothing here comes close.
    if (count == 0uL) return rng.next().toInt()
    return range.first + high64(rng.next(), count).toInt()
}

/** As above, for Swift's `randomElement(using:)` over `0..<count`. */
fun randomIndex(count: Int, rng: Mulberry32): Int = randomIn(0..(count - 1), rng)

object ProblemFactory {

    fun make(operation: Operation, level: Int, rng: Mulberry32): Problem = when (operation) {
        Operation.ADD -> {
            val r = Difficulty.additive(level)
            val a = randomIn(r, rng)
            val b = randomIn(r, rng)
            Problem(Operation.ADD, "$a + $b", a + b)
        }

        Operation.SUB -> {
            val r = Difficulty.additive(level)
            var a = randomIn(r, rng)
            var b = randomIn(r, rng)
            // Never negative, which keeps the keypad digit-only.
            if (b > a) { val t = a; a = b; b = t }
            Problem(Operation.SUB, "$a − $b", a - b)
        }

        Operation.MUL -> {
            val (lr, rr) = Difficulty.multiplicative(level)
            val a = randomIn(lr, rng)
            val b = randomIn(rr, rng)
            Problem(Operation.MUL, "$a × $b", a * b)
        }

        Operation.DIV -> {
            val (qr, dr) = Difficulty.multiplicative(level)
            // Draw order matters for parity: the divisor comes off the stream first.
            val divisor = randomIn(dr, rng)
            val quotient = randomIn(qr, rng)
            Problem(Operation.DIV, "${divisor * quotient} ÷ $divisor", quotient)
        }
    }

    /**
     * Picks an operation at random, avoiding an immediate repeat when the user
     * has more than one enabled — back-to-back identical drills feel sloppy.
     */
    fun next(ops: List<Operation>, level: Int, avoiding: Operation?, rng: Mulberry32): Problem {
        val pool = ops.ifEmpty { listOf(Operation.ADD) }
        val candidates = if (pool.size > 1 && avoiding != null) pool.filter { it != avoiding } else pool
        val op = candidates[randomIndex(candidates.size, rng)]
        return make(op, level, rng)
    }
}
