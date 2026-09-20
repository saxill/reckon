import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// Everything that survives a relaunch. Plain Codable in UserDefaults: the whole
/// blob is a few kilobytes and never leaves the device.
struct SaveData: Codable {
    var bestSprint: Int = 0
    var bestMarathon: TimeInterval?
    var dailyDate: String = ""
    var dailyScore: Int = 0
    var dailyStreak: Int = 0
    var dailyLastPlayed: String = ""
    /// Mean seconds-per-solve for the last rounds, oldest first.
    var pace: [TimeInterval] = []
    var perOperation: [String: OpStats] = [:]
    var operations: [Operation] = [.add, .sub, .mul]
    var level: Int = 2

    struct OpStats: Codable {
        var attempts: Int = 0
        var solved: Int = 0
        var totalSeconds: TimeInterval = 0
        var average: TimeInterval? { solved == 0 ? nil : totalSeconds / Double(solved) }
        var accuracy: Double? { attempts == 0 ? nil : Double(solved) / Double(attempts) }
    }
}


// MARK: - Day keys

enum DayKey {
    static var today: String { key(for: Date()) }
    static var yesterday: String {
        key(for: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date())
    }
    static func key(for date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }
}

// MARK: - Shared storage

/// The app and the widget are separate processes, so the save blob lives in an
/// App Group container that both can reach.
enum ReckonDefaults {
    static let suiteName = "group.com.sahilchanna.reckon"
    static let key = "reckon.save.v1"

    static var store: UserDefaults { UserDefaults(suiteName: suiteName) ?? .standard }

    static func load() -> SaveData {
        if let raw = store.data(forKey: key),
           let decoded = try? JSONDecoder().decode(SaveData.self, from: raw) {
            return decoded
        }
        // One-time lift from the pre-widget builds, which wrote to the app's own
        // defaults. Harmless once the group copy exists.
        if let legacy = UserDefaults.standard.data(forKey: key),
           let decoded = try? JSONDecoder().decode(SaveData.self, from: legacy) {
            store.set(legacy, forKey: key)
            return decoded
        }
        return SaveData()
    }

    static func save(_ data: SaveData) {
        guard let raw = try? JSONEncoder().encode(data) else { return }
        store.set(raw, forKey: key)
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }
}

// MARK: - Read-only views onto the save blob (used by the widget too)

extension SaveData {
    var playedDailyToday: Bool { dailyDate == DayKey.today }

    /// Median seconds-per-solve across recorded rounds — the headline number.
    var medianPace: TimeInterval? {
        guard !pace.isEmpty else { return nil }
        let s = pace.sorted()
        let mid = s.count / 2
        return s.count % 2 == 1 ? s[mid] : (s[mid - 1] + s[mid]) / 2
    }

    func stats(for op: Operation, minimumSolved: Int = 3) -> OpStats? {
        guard let s = perOperation[op.rawValue], s.solved >= minimumSolved else { return nil }
        return s
    }

    /// The operation the user is slowest at, once there is enough evidence.
    var weakestOperation: (operation: Operation, average: TimeInterval)? {
        var worst: (Operation, TimeInterval)?
        for op in Operation.allCases {
            guard let s = stats(for: op), let avg = s.average else { continue }
            if worst == nil || avg > worst!.1 { worst = (op, avg) }
        }
        return worst.map { (operation: $0.0, average: $0.1) }
    }
}
