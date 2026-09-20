import Foundation

enum GameMode: String, Codable, Identifiable, CaseIterable {
    case sprint, marathon, daily

    var id: String { rawValue }

    var title: String {
        switch self {
        case .sprint: "Sprint"
        case .marathon: "Marathon"
        case .daily: "Daily"
        }
    }

    var blurb: String {
        switch self {
        case .sprint: "60 seconds. Every miss costs you two."
        case .marathon: "20 problems, 3 lives, clock running."
        case .daily: "15 problems. Same set for everyone, all day."
        }
    }

    var icon: String {
        switch self {
        case .sprint: "bolt.fill"
        case .marathon: "flag.pattern.checkered"
        case .daily: "calendar"
        }
    }

    /// Wall-clock limit, if the mode is against the clock.
    var limit: TimeInterval? { self == .sprint ? 60 : nil }

    /// Fixed problem count, if the mode is a fixed set.
    var total: Int? {
        switch self {
        case .sprint: nil
        case .marathon: 20
        case .daily: 15
        }
    }

    var lives: Int? { self == .sprint ? nil : 3 }

    /// Seconds added to the clock for a miss (Sprint only).
    var missPenalty: TimeInterval { self == .sprint ? 2 : 0 }
}

struct RoundResult: Identifiable {
    let id = UUID()
    let mode: GameMode
    let correct: Int
    let wrong: Int
    let elapsed: TimeInterval
    let ranOutOfLives: Bool
    let solveTimes: [TimeInterval]
    let misses: [Miss]
    let log: [Attempt]
    var isPersonalBest = false

    struct Miss: Identifiable {
        let id = UUID()
        let question: String
        let answer: Int
        let typed: String
    }

    struct Attempt {
        let operation: Operation
        let seconds: TimeInterval
        let correct: Bool
    }

    var seen: Int { correct + wrong }
    var accuracy: Double? { seen == 0 ? nil : Double(correct) / Double(seen) }
    var averageSolve: TimeInterval? {
        solveTimes.isEmpty ? nil : solveTimes.reduce(0, +) / Double(solveTimes.count)
    }
    var fastestSolve: TimeInterval? { solveTimes.min() }
}
