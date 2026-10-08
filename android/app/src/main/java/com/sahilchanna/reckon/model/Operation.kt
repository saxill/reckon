package com.sahilchanna.reckon.model

/**
 * The four drills. [rawValue] is the persistence key, so don't rename it — the
 * saved blob from a previous install is read back through it.
 */
enum class Operation(val rawValue: String, val symbol: String, val title: String) {
    ADD("add", "+", "Addition"),
    SUB("sub", "−", "Subtraction"),
    MUL("mul", "×", "Multiplication"),
    DIV("div", "÷", "Division");

    companion object {
        fun fromRaw(raw: String): Operation? = entries.firstOrNull { it.rawValue == raw }
    }
}

/**
 * Difficulty 1..5. Operand ranges per level, lifted from the original web build
 * so scores stay comparable.
 */
object Difficulty {
    val range = 1..5

    /** Inclusive operand bounds for + and −. */
    fun additive(level: Int): IntRange = when (level.coerceIn(range)) {
        1 -> 2..20
        2 -> 10..99
        3 -> 20..199
        4 -> 100..999
        else -> 1000..9999
    }

    /** Inclusive bounds for (left, right) of × and ÷. */
    fun multiplicative(level: Int): Pair<IntRange, IntRange> = when (level.coerceIn(range)) {
        1 -> 2..9 to 2..9
        2 -> 2..12 to 2..9
        3 -> 3..19 to 3..9
        4 -> 11..49 to 3..12
        else -> 12..99 to 11..29
    }

    fun label(level: Int): String = when (level.coerceIn(range)) {
        1 -> "Warm up"
        2 -> "Steady"
        3 -> "Brisk"
        4 -> "Hard"
        else -> "Brutal"
    }
}
