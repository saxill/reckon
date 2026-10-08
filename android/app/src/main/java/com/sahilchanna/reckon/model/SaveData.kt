package com.sahilchanna.reckon.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Per-operation tally behind the "your slowest drill" line. */
@Serializable
data class OpStats(
    val attempts: Int = 0,
    val solved: Int = 0,
    val totalSeconds: Double = 0.0,
) {
    val average: Double? get() = if (solved == 0) null else totalSeconds / solved
    val accuracy: Double? get() = if (attempts == 0) null else solved.toDouble() / attempts
}

/**
 * Everything that survives a relaunch. The whole blob is a few kilobytes and
 * never leaves the device.
 *
 * `operations` is stored as raw strings rather than enum names so the JSON stays
 * legible and matches what the iOS build writes for the same field.
 */
@Serializable
data class SaveData(
    val bestSprint: Int = 0,
    val bestMarathon: Double? = null,
    val dailyDate: String = "",
    val dailyScore: Int = 0,
    val dailyStreak: Int = 0,
    val dailyLastPlayed: String = "",
    /** Mean seconds-per-solve per recorded round, oldest first. */
    val pace: List<Double> = emptyList(),
    val perOperation: Map<String, OpStats> = emptyMap(),
    val operations: List<String> = listOf("add", "sub", "mul"),
    val level: Int = 2,
) {
    val enabledOperations: List<Operation>
        get() = operations.mapNotNull(Operation::fromRaw)

    val playedDailyToday: Boolean get() = dailyDate == DayKey.today

    /** Median seconds-per-solve across recorded rounds — the headline number. */
    val medianPace: Double?
        get() {
            if (pace.isEmpty()) return null
            val s = pace.sorted()
            val mid = s.size / 2
            return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2
        }

    /** Null until there is enough evidence to be worth showing. */
    fun statsFor(op: Operation, minimumSolved: Int = 3): OpStats? =
        perOperation[op.rawValue]?.takeIf { it.solved >= minimumSolved }

    /** The operation the user is slowest at, once there is enough evidence. */
    val weakestOperation: Pair<Operation, Double>?
        get() {
            var worst: Pair<Operation, Double>? = null
            for (op in Operation.entries) {
                val avg = statsFor(op)?.average ?: continue
                if (worst == null || avg > worst.second) worst = op to avg
            }
            return worst
        }
}

/** `yyyy-MM-dd`, the key the Daily is seeded from and the streak is counted in. */
object DayKey {
    fun key(date: LocalDate): String =
        "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

    val today: String get() = key(LocalDate.now())
    val yesterday: String get() = key(LocalDate.now().minusDays(1))
}
