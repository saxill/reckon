package com.sahilchanna.reckon.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * The save blob, its mutations, and the derived numbers the screens read.
 *
 * On iOS this lives in an App Group so the widget's separate process can read
 * it. Android has no such split — a Glance widget runs inside the app's own
 * process — so a plain SharedPreferences file is already shared, and the widget
 * just collects [data].
 */
class Store(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("reckon.save.v1", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<SaveData> = _data.asStateFlow()

    private fun load(): SaveData =
        prefs.getString(KEY, null)
            ?.let { runCatching { json.decodeFromString<SaveData>(it) }.getOrNull() }
            ?: SaveData()

    private fun persist(next: SaveData) {
        prefs.edit().putString(KEY, json.encodeToString(SaveData.serializer(), next)).apply()
        _data.value = next
    }

    // MARK: settings

    fun toggle(op: Operation) {
        val current = _data.value
        val ops = current.enabledOperations.toMutableList()
        if (ops.contains(op)) {
            // Never leave zero drills enabled.
            if (ops.size <= 1) return
            ops.remove(op)
        } else {
            ops.add(op)
        }
        persist(current.copy(operations = ops.sortedBy { it.ordinal }.map { it.rawValue }))
    }

    fun setLevel(level: Int) {
        persist(_data.value.copy(level = level.coerceIn(Difficulty.range)))
    }

    fun reset() = persist(SaveData())

    // MARK: recording

    /**
     * Folds a finished round into the saved history. Returns the round with
     * `isPersonalBest` set, so the result screen can celebrate it.
     */
    fun record(result: RoundResult): RoundResult {
        var current = _data.value
        var isPersonalBest = false

        when (result.mode) {
            GameMode.SPRINT ->
                if (result.correct > current.bestSprint) {
                    current = current.copy(bestSprint = result.correct)
                    isPersonalBest = true
                }

            GameMode.MARATHON ->
                if (!result.ranOutOfLives &&
                    (current.bestMarathon == null || result.elapsed < current.bestMarathon!!)
                ) {
                    current = current.copy(bestMarathon = result.elapsed)
                    isPersonalBest = true
                }

            GameMode.DAILY ->
                if (current.dailyDate != DayKey.today) {
                    val streak =
                        if (current.dailyLastPlayed == DayKey.yesterday) current.dailyStreak + 1 else 1
                    current = current.copy(
                        dailyStreak = streak,
                        dailyLastPlayed = DayKey.today,
                        dailyDate = DayKey.today,
                        dailyScore = result.correct,
                    )
                }
        }

        val perOp = current.perOperation.toMutableMap()
        for (attempt in result.log) {
            val s = perOp[attempt.operation.rawValue] ?: OpStats()
            perOp[attempt.operation.rawValue] = s.copy(
                attempts = s.attempts + 1,
                solved = s.solved + if (attempt.correct) 1 else 0,
                totalSeconds = s.totalSeconds + if (attempt.correct) attempt.seconds else 0.0,
            )
        }
        current = current.copy(perOperation = perOp)

        result.averageSolve?.let { avg ->
            val pace = (current.pace + avg).let {
                if (it.size > PACE_WINDOW) it.takeLast(PACE_WINDOW) else it
            }
            current = current.copy(pace = pace)
        }

        persist(current)
        return result.copy(isPersonalBest = isPersonalBest)
    }

    private companion object {
        const val KEY = "reckon.save.v1"
        const val PACE_WINDOW = 14
    }
}
