package com.sahilchanna.reckon.model

enum class GameMode(val rawValue: String, val title: String, val blurb: String) {
    SPRINT("sprint", "Sprint", "60 seconds. Every miss costs you two."),
    MARATHON("marathon", "Marathon", "20 problems, 3 lives, clock running."),
    DAILY("daily", "Daily", "15 problems. Same set for everyone, all day.");

    /** Wall-clock limit, if the mode is against the clock. */
    val limit: Double? get() = if (this == SPRINT) 60.0 else null

    /** Fixed problem count, if the mode is a fixed set. */
    val total: Int? get() = when (this) {
        SPRINT -> null
        MARATHON -> 20
        DAILY -> 15
    }

    val lives: Int? get() = if (this == SPRINT) null else 3

    /** Seconds added to the clock for a miss (Sprint only). */
    val missPenalty: Double get() = if (this == SPRINT) 2.0 else 0.0
}

/** A problem the user got wrong, with what they actually typed. */
data class Miss(val question: String, val answer: Int, val typed: String)

/** One answered problem, for the per-operation profile. */
data class Attempt(val operation: Operation, val seconds: Double, val correct: Boolean)

data class RoundResult(
    val mode: GameMode,
    val correct: Int,
    val wrong: Int,
    val elapsed: Double,
    val ranOutOfLives: Boolean,
    val solveTimes: List<Double>,
    val misses: List<Miss>,
    val log: List<Attempt>,
    val isPersonalBest: Boolean = false,
) {
    val seen: Int get() = correct + wrong
    val accuracy: Double? get() = if (seen == 0) null else correct.toDouble() / seen
    val averageSolve: Double?
        get() = if (solveTimes.isEmpty()) null else solveTimes.sum() / solveTimes.size
    val fastestSolve: Double? get() = solveTimes.minOrNull()
}
