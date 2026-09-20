import Foundation
import Observation

/// App-side wrapper over the shared save blob: mutations, round recording and
/// the derived numbers the screens read. The blob itself (and the widget's
/// read-only view of it) lives in Shared/SaveData.swift.
@Observable
final class Store {
    private static let paceWindow = 14

    private(set) var data: SaveData

    init() {
        data = ReckonDefaults.load()
        #if DEBUG
        // `-seedDemo` fills the app and the widget with plausible history for
        // screenshots. It writes to the App Group, so it does replace real
        // scores on that simulator or device — debug builds only.
        if CommandLine.arguments.contains("-seedDemo") {
            data = .demo
            ReckonDefaults.save(data)
        }
        #endif
    }

    // MARK: settings

    func toggle(_ op: Operation) {
        if let i = data.operations.firstIndex(of: op) {
            guard data.operations.count > 1 else { return }   // never leave zero drills
            data.operations.remove(at: i)
        } else {
            data.operations.append(op)
        }
        data.operations.sort { Operation.allCases.firstIndex(of: $0)! < Operation.allCases.firstIndex(of: $1)! }
        persist()
    }

    func setLevel(_ level: Int) {
        data.level = level.clamped(to: Difficulty.range)
        persist()
    }

    func reset() {
        data = SaveData()
        persist()
    }

    // MARK: derived

    static var todayKey: String { DayKey.today }
    var playedDailyToday: Bool { data.playedDailyToday }
    var medianPace: TimeInterval? { data.medianPace }
    var weakestOperation: (operation: Operation, average: TimeInterval)? { data.weakestOperation }
    func stats(for op: Operation) -> SaveData.OpStats? { data.stats(for: op) }

    // MARK: recording

    /// Folds a finished round into the saved history. Returns the round with
    /// `isPersonalBest` set, so the result screen can celebrate it.
    @discardableResult
    func record(_ result: RoundResult) -> RoundResult {
        var out = result

        switch result.mode {
        case .sprint:
            if result.correct > data.bestSprint {
                data.bestSprint = result.correct
                out.isPersonalBest = true
            }
        case .marathon:
            if !result.ranOutOfLives, data.bestMarathon == nil || result.elapsed < data.bestMarathon! {
                data.bestMarathon = result.elapsed
                out.isPersonalBest = true
            }
        case .daily:
            if data.dailyDate != DayKey.today {
                data.dailyStreak = (data.dailyLastPlayed == DayKey.yesterday) ? data.dailyStreak + 1 : 1
                data.dailyLastPlayed = DayKey.today
                data.dailyDate = DayKey.today
                data.dailyScore = result.correct
            }
        }

        for attempt in result.log {
            var s = data.perOperation[attempt.operation.rawValue] ?? SaveData.OpStats()
            s.attempts += 1
            if attempt.correct {
                s.solved += 1
                s.totalSeconds += attempt.seconds
            }
            data.perOperation[attempt.operation.rawValue] = s
        }

        if let avg = result.averageSolve {
            data.pace.append(avg)
            if data.pace.count > Self.paceWindow { data.pace.removeFirst(data.pace.count - Self.paceWindow) }
        }

        persist()
        return out
    }

    /// Writes through to the App Group and nudges WidgetKit to redraw.
    private func persist() { ReckonDefaults.save(data) }
}

#if DEBUG
extension SaveData {
    static var demo: SaveData {
        var d = SaveData()
        d.bestSprint = 24
        d.bestMarathon = 41.2
        d.dailyStreak = 3
        d.dailyLastPlayed = DayKey.yesterday
        d.pace = [2.41, 2.28, 2.35, 2.12, 2.19, 1.96, 2.04, 1.88]
        d.perOperation = [
            "add": .init(attempts: 96, solved: 92, totalSeconds: 142.6),
            "sub": .init(attempts: 74, solved: 68, totalSeconds: 149.9),
            "mul": .init(attempts: 81, solved: 70, totalSeconds: 221.2),
            "div": .init(attempts: 22, solved: 18, totalSeconds: 74.5),
        ]
        return d
    }
}
#endif
