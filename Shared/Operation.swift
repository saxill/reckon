import Foundation

/// The four drills. Raw values are the persistence keys, so don't rename them.
enum Operation: String, CaseIterable, Codable, Identifiable, Sendable {
    case add, sub, mul, div

    var id: String { rawValue }

    var symbol: String {
        switch self {
        case .add: "+"
        case .sub: "−"
        case .mul: "×"
        case .div: "÷"
        }
    }

    var title: String {
        switch self {
        case .add: "Addition"
        case .sub: "Subtraction"
        case .mul: "Multiplication"
        case .div: "Division"
        }
    }
}

/// Difficulty 1...5. Operand ranges per level, lifted from the original web build
/// so scores stay comparable.
enum Difficulty {
    static let range = 1...5

    /// Inclusive operand bounds for + and −.
    static func additive(_ level: Int) -> ClosedRange<Int> {
        switch level.clamped(to: range) {
        case 1: 2...20
        case 2: 10...99
        case 3: 20...199
        case 4: 100...999
        default: 1000...9999
        }
    }

    /// Inclusive bounds for (left, right) of × and ÷.
    static func multiplicative(_ level: Int) -> (ClosedRange<Int>, ClosedRange<Int>) {
        switch level.clamped(to: range) {
        case 1: (2...9, 2...9)
        case 2: (2...12, 2...9)
        case 3: (3...19, 3...9)
        case 4: (11...49, 3...12)
        default: (12...99, 11...29)
        }
    }

    static func label(_ level: Int) -> String {
        switch level.clamped(to: range) {
        case 1: "Warm up"
        case 2: "Steady"
        case 3: "Brisk"
        case 4: "Hard"
        default: "Brutal"
        }
    }
}

extension Comparable {
    func clamped(to r: ClosedRange<Self>) -> Self { min(max(self, r.lowerBound), r.upperBound) }
}
