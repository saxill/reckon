// Ground truth for the Android port.
//
// The Daily problem set is seeded from the date so that every device gets the
// same 15 problems. Reproducing that on Android means reproducing two things
// exactly: the mulberry32 stream in `SeededRandom`, and whatever uniform-int
// method Swift's `Int.random(in:using:)` applies on top of it. The first is
// arithmetic and certain; the second is a Swift stdlib implementation detail.
//
// This harness runs the *real* iOS sources (Operation.swift + Problem.swift,
// concatenated in front of this file) and prints both layers, so the Kotlin
// port can be checked against actual output instead of a guess.

import Foundation

let key = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "2026-10-08"

print("dateKey: \(key)")

// Layer 1 — the raw 64-bit stream, which the Kotlin RNG must match bit for bit.
print("\n== raw next() stream ==")
var raw = SeededRandom(dateKey: key)
for i in 0..<6 {
    print("  [\(i)] \(raw.next())")
}

// Layer 2 — the uniform draws Swift layers on top, across the bounds the
// problem factory actually uses.
print("\n== Int.random(in:) over factory bounds ==")
for bound in [(2, 20), (10, 99), (2, 9), (3, 19), (11, 49), (12, 99), (1000, 9999)] {
    var g = SeededRandom(dateKey: key)
    let draws = (0..<4).map { _ in Int.random(in: bound.0...bound.1, using: &g) }
    print("  \(bound.0)...\(bound.1): \(draws)")
}

// Layer 3 — the actual thing that must match: today's Daily set.
print("\n== Daily set (level 3, all ops) ==")
var rng = SeededRandom(dateKey: key)
var last: Operation?
for i in 0..<15 {
    let p = ProblemFactory.next(from: Operation.allCases, level: 3, avoiding: last, using: &rng)
    print("  \(i + 1). \(p.question) = \(p.answer)")
    last = p.operation
}
