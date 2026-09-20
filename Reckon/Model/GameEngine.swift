import Foundation
import Observation

/// Concrete wrapper so the engine can hold either the system generator or the
/// date-seeded one without reaching for an existential.
enum RandomSource: RandomNumberGenerator {
    case system(SystemRandomNumberGenerator)
    case seeded(SeededRandom)

    mutating func next() -> UInt64 {
        switch self {
        case .system(var g): let v = g.next(); self = .system(g); return v
        case .seeded(var g): let v = g.next(); self = .seeded(g); return v
        }
    }
}

@Observable
final class GameEngine {
    enum Feedback: Equatable { case idle, correct, wrong }

    let mode: GameMode
    let total: Int?
    let ops: [Operation]

    private(set) var problem: Problem
    private(set) var typed = ""
    private(set) var answered = 0
    private(set) var correct = 0
    private(set) var wrong = 0
    private(set) var livesLeft: Int
    private(set) var feedback: Feedback = .idle
    private(set) var elapsed: TimeInterval = 0
    private(set) var isFinished = false
    /// Consecutive correct answers — drives the little streak flourish.
    private(set) var runStreak = 0

    private let level: Int
    private var rng: RandomSource
    private var startDate = Date()
    private var questionStart = Date()
    private var penalty: TimeInterval = 0
    private var isLocked = false
    private var timer: Timer?
    private var solveTimes: [TimeInterval] = []
    private var misses: [RoundResult.Miss] = []
    private var log: [RoundResult.Attempt] = []
    private var onFinish: ((RoundResult) -> Void)?

    init(mode: GameMode, operations: [Operation], level: Int) {
        // Every stored property has to land before `self` is touched, so the
        // first problem is drawn from a local generator and moved in after.
        let resolvedOps: [Operation]
        let resolvedLevel: Int
        var source: RandomSource

        if mode == .daily {
            // Everyone gets the same 15 problems for a given day.
            resolvedOps = Operation.allCases
            resolvedLevel = 3
            source = .seeded(SeededRandom(dateKey: Store.todayKey))
        } else {
            resolvedOps = operations.isEmpty ? [.add] : operations
            resolvedLevel = level
            source = .system(SystemRandomNumberGenerator())
        }

        let first = ProblemFactory.next(from: resolvedOps, level: resolvedLevel, avoiding: nil, using: &source)

        self.mode = mode
        self.total = mode.total
        self.livesLeft = mode.lives ?? 0
        self.ops = resolvedOps
        self.level = resolvedLevel
        self.rng = source
        self.problem = first
    }

    // MARK: lifecycle

    func start(onFinish: @escaping (RoundResult) -> Void) {
        self.onFinish = onFinish
        startDate = Date()
        questionStart = Date()
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            self?.tick()
        }
    }

    func abandon() {
        timer?.invalidate()
        timer = nil
        onFinish = nil
        isFinished = true
    }

    deinit { timer?.invalidate() }

    private func tick() {
        guard !isFinished else { return }
        elapsed = Date().timeIntervalSince(startDate) + penalty
        if let limit = mode.limit, elapsed >= limit { finish() }
    }

    // MARK: display

    var remaining: TimeInterval? { mode.limit.map { max(0, $0 - elapsed) } }

    var clockText: String {
        if let remaining {
            return String(format: "0:%02d", Int(ceil(remaining)))
        }
        let s = Int(elapsed)
        return String(format: "%d:%02d", s / 60, s % 60)
    }

    /// 0...1, how full the progress bar should be.
    var progress: Double {
        if let limit = mode.limit, let remaining { return max(0, min(1, remaining / limit)) }
        guard let total else { return 0 }
        return max(0, min(1, Double(answered) / Double(total)))
    }

    // MARK: input

    func type(_ digit: String) {
        guard !isLocked, !isFinished, typed.count < 7 else { return }
        if typed == "0" { typed = "" }
        typed += digit
        Haptics.tap()
        if typed.count >= problem.digits { submit() }
    }

    func backspace() {
        guard !isLocked, !typed.isEmpty else { return }
        typed.removeLast()
        Haptics.tap()
    }

    func clear() {
        guard !isLocked, !typed.isEmpty else { return }
        typed = ""
        Haptics.tap()
    }

    /// Submit whatever is typed, for the Return key. The on-screen keypad
    /// relies on the digit-count auto-submit instead.
    func commit() {
        guard !isLocked, !isFinished, !typed.isEmpty else { return }
        submit()
    }

    private func submit() {
        guard let value = Int(typed) else { return }
        let seconds = Date().timeIntervalSince(questionStart)
        let ok = value == problem.answer

        answered += 1
        log.append(.init(operation: problem.operation, seconds: seconds, correct: ok))

        if ok {
            correct += 1
            runStreak += 1
            solveTimes.append(seconds)
            feedback = .correct
            Haptics.success()
        } else {
            wrong += 1
            runStreak = 0
            livesLeft = max(0, livesLeft - 1)
            penalty += mode.missPenalty
            misses.append(.init(question: problem.question, answer: problem.answer, typed: typed))
            feedback = .wrong
            Haptics.error()
        }

        if mode != .sprint, livesLeft <= 0 || answered >= (total ?? 0) {
            // Let the last answer register on screen before the result slides in.
            isLocked = true
            DispatchQueue.main.asyncAfter(deadline: .now() + (ok ? 0.28 : 0.6)) { [weak self] in
                self?.finish()
            }
            return
        }

        isLocked = true
        DispatchQueue.main.asyncAfter(deadline: .now() + (ok ? 0.14 : 0.52)) { [weak self] in
            self?.advance()
        }
    }

    private func advance() {
        guard !isFinished else { return }
        var g = rng
        problem = ProblemFactory.next(from: ops, level: level, avoiding: problem.operation, using: &g)
        rng = g
        typed = ""
        feedback = .idle
        isLocked = false
        questionStart = Date()
    }

    private func finish() {
        guard !isFinished else { return }
        isFinished = true
        timer?.invalidate()
        timer = nil

        let result = RoundResult(
            mode: mode,
            correct: correct,
            wrong: wrong,
            elapsed: Date().timeIntervalSince(startDate),
            ranOutOfLives: mode.lives != nil && livesLeft <= 0,
            solveTimes: solveTimes,
            misses: misses,
            log: log
        )
        let finish = onFinish
        onFinish = nil
        finish?(result)
    }
}
