package com.sahilchanna.reckon.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.random.nextUInt

/**
 * One round: timing, input, scoring, lives.
 *
 * The iOS build drives this off a `Timer` and `DispatchQueue.main.asyncAfter`.
 * Neither exists here, so the clock is a monotonic [now] the UI polls and the
 * post-answer pauses are coroutines on [scope] — same timings, which are part of
 * how the round feels.
 */
class GameEngine(
    val mode: GameMode,
    operations: List<Operation>,
    level: Int,
    private val scope: CoroutineScope,
    dateKey: String = DayKey.today,
    private val now: () -> Double = { System.nanoTime() / 1_000_000_000.0 },
) {
    enum class Feedback { IDLE, CORRECT, WRONG }

    val total: Int? = mode.total
    val ops: List<Operation>
    private val level: Int
    private val rng: Mulberry32

    var problem by mutableStateOf(Problem(Operation.ADD, "", 0))
        private set
    var typed by mutableStateOf("")
        private set
    var answered by mutableStateOf(0)
        private set
    var correct by mutableStateOf(0)
        private set
    var wrong by mutableStateOf(0)
        private set
    var livesLeft by mutableStateOf(mode.lives ?: 0)
        private set
    var feedback by mutableStateOf(Feedback.IDLE)
        private set
    var elapsed by mutableStateOf(0.0)
        private set
    var isFinished by mutableStateOf(false)
        private set

    /** Consecutive correct answers — drives the little streak flourish. */
    var runStreak by mutableStateOf(0)
        private set

    private var startAt = now()
    private var questionStart = startAt
    private var penalty = 0.0
    private var isLocked = false
    private var onFinish: ((RoundResult) -> Unit)? = null
    private val solveTimes = mutableListOf<Double>()
    private val misses = mutableListOf<Miss>()
    private val log = mutableListOf<Attempt>()

    init {
        if (mode == GameMode.DAILY) {
            // Everyone gets the same 15 problems for a given day.
            ops = Operation.entries.toList()
            this.level = 3
            rng = Mulberry32.forDate(dateKey)
        } else {
            ops = operations.ifEmpty { listOf(Operation.ADD) }
            this.level = level
            rng = Mulberry32(Random.nextUInt())
        }
        problem = ProblemFactory.next(ops, this.level, null, rng)
    }

    // MARK: lifecycle

    fun start(onFinish: (RoundResult) -> Unit) {
        this.onFinish = onFinish
        startAt = now()
        questionStart = startAt
        elapsed = 0.0
    }

    fun abandon() {
        onFinish = null
        isFinished = true
    }

    /** Called from the UI's frame loop while the round is live. */
    fun tick() {
        if (isFinished) return
        elapsed = now() - startAt + penalty
        mode.limit?.let { if (elapsed >= it) finish() }
    }

    // MARK: display

    val remaining: Double? get() = mode.limit?.let { maxOf(0.0, it - elapsed) }

    val clockText: String
        get() {
            val r = remaining
            if (r != null) return "0:%02d".format(kotlin.math.ceil(r).toInt())
            val s = elapsed.toInt()
            return "%d:%02d".format(s / 60, s % 60)
        }

    /** 0..1, how full the progress bar should be. */
    val progress: Double
        get() {
            val limit = mode.limit
            val r = remaining
            if (limit != null && r != null) return (r / limit).coerceIn(0.0, 1.0)
            val t = total ?: return 0.0
            return (answered.toDouble() / t).coerceIn(0.0, 1.0)
        }

    // MARK: input

    fun type(digit: String) {
        if (isLocked || isFinished || typed.length >= 7) return
        if (typed == "0") typed = ""
        typed += digit
        if (typed.length >= problem.digits) submit()
    }

    fun backspace() {
        if (isLocked || typed.isEmpty()) return
        typed = typed.dropLast(1)
    }

    fun clear() {
        if (isLocked || typed.isEmpty()) return
        typed = ""
    }

    /**
     * Submit whatever is typed, for the Return key. The on-screen keypad relies
     * on the digit-count auto-submit instead.
     */
    fun commit() {
        if (isLocked || isFinished || typed.isEmpty()) return
        submit()
    }

    private fun submit() {
        val value = typed.toIntOrNull() ?: return
        val seconds = now() - questionStart
        val ok = value == problem.answer

        answered += 1
        log += Attempt(problem.operation, seconds, ok)

        if (ok) {
            correct += 1
            runStreak += 1
            solveTimes += seconds
            feedback = Feedback.CORRECT
        } else {
            wrong += 1
            runStreak = 0
            livesLeft = maxOf(0, livesLeft - 1)
            penalty += mode.missPenalty
            misses += Miss(problem.question, problem.answer, typed)
            feedback = Feedback.WRONG
        }

        if (mode != GameMode.SPRINT && (livesLeft <= 0 || answered >= (total ?: 0))) {
            // Let the last answer register on screen before the result slides in.
            isLocked = true
            scope.launch {
                delay(if (ok) 280 else 600)
                finish()
            }
            return
        }

        isLocked = true
        scope.launch {
            delay(if (ok) 140 else 520)
            advance()
        }
    }

    private fun advance() {
        if (isFinished) return
        problem = ProblemFactory.next(ops, level, problem.operation, rng)
        typed = ""
        feedback = Feedback.IDLE
        isLocked = false
        questionStart = now()
    }

    private fun finish() {
        if (isFinished) return
        isFinished = true

        val result = RoundResult(
            mode = mode,
            correct = correct,
            wrong = wrong,
            elapsed = now() - startAt,
            ranOutOfLives = mode.lives != null && livesLeft <= 0,
            solveTimes = solveTimes.toList(),
            misses = misses.toList(),
            log = log.toList(),
        )
        val finish = onFinish
        onFinish = null
        finish?.invoke(result)
    }
}
