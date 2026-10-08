# Architecture

How Reckon is put together. The two apps are separate codebases with the same
shape: a small pure model layer, a round engine, a store, and a UI that reads
both. Nothing below is platform-specific unless it says so.

## The domain

An **operation** is one of the four drills — addition, subtraction,
multiplication, division (`ios/Shared/Operation.swift`,
`android/.../model/Operation.kt`). Its raw value (`add`, `sub`, `mul`, `div`) is
the persistence key, so it appears in the saved blob and must not be renamed.

A **difficulty band** is a level 1–5 that selects operand ranges. Both platforms
carry the same table, lifted from the original web build so scores stay
comparable (`Difficulty` in the same two files):

| Level | Label | + / − operands | × / ÷ left | × / ÷ right |
|---|---|---|---|---|
| 1 | Warm up | 2…20 | 2…9 | 2…9 |
| 2 | Steady | 10…99 | 2…12 | 2…9 |
| 3 | Brisk | 20…199 | 3…19 | 3…9 |
| 4 | Hard | 100…999 | 11…49 | 3…12 |
| 5 | Brutal | 1000…9999 | 12…99 | 11…29 |

For `÷` the "left" range is the quotient and the "right" range the divisor; for
`×` they are the two factors. The level is clamped to 1…5 at every entry point.

A **Problem** is immutable: an operation, the question as a display string, and
the integer answer (`ios/Reckon/Model/Problem.swift`,
`android/.../model/Problem.kt`). `digits` — the length of the answer — is what
the keypad auto-submits on.

A **round** is one play of a mode. `GameMode` holds the rules (a `limit` for
Sprint, a `total` problem count for Marathon and Daily, `lives` for Marathon and
Daily, a `missPenalty` for Sprint) and `RoundResult` is the in-memory product of
a finished round: correct/wrong counts, elapsed time, per-problem solve times,
the misses, and a per-attempt log (`ios/Reckon/Model/GameMode.swift`,
`android/.../model/GameMode.kt`).

A **saved result** is not the `RoundResult`. Finishing a round hands it to
`Store.record`, which folds it into aggregates and discards the rest
(`ios/Reckon/Model/Store.swift`, `android/.../model/Store.kt`). The **history**
the headline number is computed over is a rolling window of the per-round mean
seconds-per-solve, capped at 14 entries.

## Problem generation

`ProblemFactory.make` builds one problem per operation (`Problem.swift` /
`Problem.kt`):

- **Addition** — two draws from the additive range, `a + b`.
- **Subtraction** — two draws from the additive range, swapped so the larger is
  left: `a − b` with `b ≤ a`.
- **Multiplication** — one draw from each multiplicative range, `a × b`.
- **Division** — a divisor from the divisor range and a quotient from the
  quotient range, asked as `divisor × quotient ÷ divisor` with the quotient as
  the answer. The divisor is drawn *first*; the order matters for Daily parity.

`ProblemFactory.next` picks an operation and then builds the problem. When more
than one operation is enabled it filters out the last operation used, so the same
drill never appears twice in a row; an empty operation list falls back to
addition.

The keypad is digits only, and two invariants keep it that way:

- **Subtraction never goes negative** — the swap above guarantees `b ≤ a`, so
  the answer needs no minus sign.
- **Division is always exact** — the dividend is constructed as
  `divisor × quotient`, so the answer is the quotient the keypad can type.

Both are asserted over 2,000 generated problems in
`DailyParityTest.generated problems stay well formed`. The engine also caps input
at 7 digits and submits once the typed length reaches the answer's `digits`.

## The Daily

The Daily is 15 problems at level 3 over all four operations, seeded from the
date so every device gets the same set. The seed is an FNV-1a hash of a
`yyyy-MM-dd` key (`DayKey`), feeding a **mulberry32** generator
(`SeededRandom` / `Mulberry32`).

The subtlety — and the reason the two ports agree at all — is that Swift's
`Int.random(in:using:)` does not take `n % count`. It uses **Lemire
multiply-shift**: the high 64 bits of the 128-bit product `n × count`. A naive
modulo port matches most draws and then silently diverges, which for a
date-seeded Daily would mean an iPhone and an Android phone disagreeing about
"today's Daily" with nothing to show for it. The Kotlin side reproduces it in
`randomIn`, computing the high 64 bits by hand because `Math.multiplyHigh` is
API 33+ and `minSdk` is 26.

This is pinned, not assumed: `DailyParityTest` checks the raw mulberry32 stream,
the uniform draws over every range the factory uses, and a whole 15-problem set,
against values produced by running the *real* Swift sources through the
`android/parity/` harness. The full story and the regeneration command are in
[`android/README.md`](../android/README.md); this page only summarises it.

## The game engine

`GameEngine` owns one round (`ios/Reckon/Model/GameEngine.swift`,
`android/.../model/GameEngine.kt`). It is constructed with a mode, the enabled
operations and a level; the Daily ignores the arguments and forces all four
operations at level 3 with the date-seeded generator, while the other modes use
the system generator.

- **Timing** — elapsed is `now − start + penalty`. Sprint finishes the instant
  elapsed reaches its 60-second limit; the other modes finish on problem count or
  lives. `penalty` grows by 2 seconds per miss in Sprint.
- **Input** — `type` appends a digit (ignoring a lone leading `0`), `backspace`
  and `clear` edit what is typed, `commit` submits a partial answer for the
  Return key. Every key submits itself automatically once the typed length
  matches the answer's `digits`.
- **Scoring** — a correct answer increments `correct`, `runStreak` and the solve
  times; a wrong one increments `wrong`, resets the streak, takes a life, adds
  the miss penalty, and records the miss with what was typed. Each attempt goes
  into the log.
- **Post-answer pauses** — after an answer the engine locks input and waits
  before advancing, 0.14 s after a correct answer and 0.52 s after a wrong one.
  When the answer is the *last* one it waits 0.28 s / 0.6 s and finishes instead,
  so the final answer registers on screen before the result appears.

Each platform implements the clock its own way, with the delays unchanged:

- **iOS** runs a `Timer` at 0.05 s whose `tick` updates elapsed and checks the
  limit, and schedules the pauses with `DispatchQueue.main.asyncAfter`.
- **Android** has no engine-owned timer. `GameScreen` pumps `engine.tick()` once
  per frame off Compose's `withFrameNanos`, and the pauses are `delay` calls on a
  coroutine scope the engine is handed (`android/.../ui/screens/GameScreen.kt`).

## Stats

Every answered problem contributes an `Attempt` — operation, seconds, whether it
was right — to the round's log. On finish, `Store.record` folds the log in:

- **Per-operation profile** — attempts, solves and total seconds per operation;
  the average is over solved problems only, the accuracy over all attempts. A
  drill needs at least 3 solves before it is reported on, and the slowest
  qualifying one is the "weakest drill", offered as a one-tap sprint.
- **Pace** — the round's mean seconds-per-solve (`averageSolve`) is appended to
  `pace`, which keeps the last 14, oldest first. The headline number is the
  **median** of that window: the middle value for an odd count, the mean of the
  middle pair otherwise. A rising line means getting faster.
- **Bests and the Daily** — a Sprint is a personal best when it beats
  `bestSprint`; a Marathon only when it was completed without running out of
  lives and is the fastest; the first Daily of a day sets the score and extends
  the streak if yesterday's was played.

History is local only. There is no network code, no accounts and no analytics on
either platform; the blob is a few kilobytes and never leaves the device. (A
finished round's `RoundResult` — including its misses and log — exists only for
the result screen; only the aggregates above are persisted.)

## Storage and the widget

The save blob is a single JSON document behind a versioned key
(`reckon.save.v1`).

- **iOS** stores it in `UserDefaults` inside the App Group
  `group.com.sahilchanna.reckon`, so the widget's separate process can read it
  (`ReckonDefaults` in `ios/Shared/SaveData.swift`). It lifts a pre-widget blob
  out of the app's own defaults once, on first load. Saving calls
  `WidgetCenter.shared.reloadAllTimelines()`.
- **Android** stores it in a private `SharedPreferences` file, named
  `reckon.save.v1` under the key `reckon.save.v1`, serialised with
  kotlinx.serialization (`android/.../model/Store.kt`). A Glance widget runs in
  the app's own process, so that file is already shared state — no App Group
  equivalent is needed. The root screen refreshes the widget whenever the save
  changes, through `ReckonWidgetRefresh` (`android/.../widget/ReckonWidget.kt`).

Both widgets show the same thing: the median solve time, the daily streak,
whether today's Daily is done (and its score), a sparkline of recent rounds, and
the Sprint and Marathon bests. Tapping opens straight into a Sprint via
`reckon://sprint`. The iOS widget is a small/medium WidgetKit extension that
refreshes on save and otherwise sits until just after midnight, when the Daily
resets; the Android widget is a responsive Glance widget with square and wide
layouts, and draws its sparkline as bars because Glance cannot draw a path.

## Testing

The Android app is the one with an automated suite — **35 unit tests, 0
failures** — and it exercises the model layer, not the UI:

- `DailyParityTest` (5) pins the Daily generator against real Swift output,
  including the Lemire draws and the well-formedness invariants.
- `EngineTest` (11) drives the engine against a hand-advanced fake clock and a
  `TestScope`, pinning scoring, lives, the Sprint miss penalty and time limit,
  auto-submit, backspace/clear/commit, and the clock and progress readouts.
- `StoreTest` (13) runs under Robolectric so the blob round-trips through a real
  `SharedPreferences`; it covers personal bests, the Daily streak, the
  per-operation profile, the 14-round pace window, and corrupt-save recovery.
- `SaveDataTest` (6) covers the derived numbers: the median, the 3-solve
  threshold, the weakest drill, and the played-today flag.

What is deliberately *not* covered: the Compose and SwiftUI screens, the widgets
on either platform, and anything that needs a real device. The iOS project has no
test target at all — the Daily's cross-platform behaviour is pinned by the
Android test, whose expectations come from the Swift sources rather than from
Kotlin.
