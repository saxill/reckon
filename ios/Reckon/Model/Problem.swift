import Foundation

struct Problem: Equatable {
    let operation: Operation
    let question: String
    let answer: Int

    var digits: Int { String(answer).count }
}

/// mulberry32 — small, fast, and seedable, so the Daily set is identical on
/// every device for a given date (same generator the web version used).
struct SeededRandom: RandomNumberGenerator {
    private var state: UInt32

    init(seed: UInt32) { state = seed }

    init(dateKey: String) {
        var h: UInt32 = 2_166_136_261
        for byte in dateKey.utf8 {
            h ^= UInt32(byte)
            h = h &* 16_777_619
        }
        state = h
    }

    private mutating func next32() -> UInt32 {
        state = state &+ 0x6D2B_79F5
        var t = state
        t = (t ^ (t >> 15)) &* (1 | t)
        t ^= t &+ (t ^ (t >> 7)) &* (61 | t)
        return t ^ (t >> 14)
    }

    mutating func next() -> UInt64 {
        (UInt64(next32()) << 32) | UInt64(next32())
    }
}

enum ProblemFactory {
    static func make(_ operation: Operation, level: Int, using rng: inout some RandomNumberGenerator) -> Problem {
        switch operation {
        case .add:
            let r = Difficulty.additive(level)
            let a = Int.random(in: r, using: &rng), b = Int.random(in: r, using: &rng)
            return Problem(operation: .add, question: "\(a) + \(b)", answer: a + b)

        case .sub:
            let r = Difficulty.additive(level)
            var a = Int.random(in: r, using: &rng), b = Int.random(in: r, using: &rng)
            if b > a { swap(&a, &b) }            // never negative: keeps the keypad digit-only
            return Problem(operation: .sub, question: "\(a) − \(b)", answer: a - b)

        case .mul:
            let (lr, rr) = Difficulty.multiplicative(level)
            let a = Int.random(in: lr, using: &rng), b = Int.random(in: rr, using: &rng)
            return Problem(operation: .mul, question: "\(a) × \(b)", answer: a * b)

        case .div:
            let (qr, dr) = Difficulty.multiplicative(level)
            let divisor = Int.random(in: dr, using: &rng)
            let quotient = Int.random(in: qr, using: &rng)
            return Problem(operation: .div, question: "\(divisor * quotient) ÷ \(divisor)", answer: quotient)
        }
    }

    /// Picks an operation at random, avoiding an immediate repeat when the user
    /// has more than one enabled — back-to-back identical drills feel sloppy.
    static func next(from ops: [Operation],
                     level: Int,
                     avoiding last: Operation?,
                     using rng: inout some RandomNumberGenerator) -> Problem {
        let pool = ops.isEmpty ? [.add] : ops
        var candidates = pool
        if pool.count > 1, let last { candidates = pool.filter { $0 != last } }
        let op = candidates.randomElement(using: &rng) ?? .add
        return make(op, level: level, using: &rng)
    }
}
