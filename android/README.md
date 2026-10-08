# Reckon — Android

Kotlin + Jetpack Compose port of the iOS app. Same maths, same scoring, same
interface, and — for the Daily — the same set of problems.

## Run it

```bash
cd ~/code/Reckon/android
./gradlew installDebug
```

Needs JDK 17 (`brew install openjdk@17`), and an Android SDK. Point
`local.properties` at it if Gradle can't find one:

```properties
sdk.dir=/usr/local/share/android-commandlinetools
```

`./gradlew testDebugUnitTest` runs the unit tests. `minSdk` is 26, `targetSdk` 35.

## The Daily's cross-platform promise

The Daily is seeded from the date so that every device gets the same 15
problems. That only holds here if the port reproduces Swift's generator
*exactly* — the mulberry32 stream, and the uniform-integer draw taken from it.

Swift's `Int.random(in:using:)` does not use `n % count`. It uses Lemire
multiply-shift: it takes `n × count` as a 128-bit product and keeps the **high
64 bits**. A naive modulo port agrees on most draws and then quietly diverges,
which is exactly the kind of bug that would make two phones disagree about
"today's Daily" without anyone noticing. `model/Problem.kt` therefore ports
mulberry32 bit for bit and implements the multiply-shift with a hand-rolled
128-bit multiply, because `Math.multiplyHigh` is API 33+ and we support 26.

`DailyParityTest.kt` pins the behaviour. Its expected values are **output from
the real iOS sources**, not from the Kotlin — regenerate them by rerunning the
harness rather than editing them to match the code:

```bash
cd ~/code/Reckon/android/parity
cat ../../ios/Shared/Operation.swift ../../ios/Reckon/Model/Problem.swift main.swift > $TMPDIR/reckon-parity.swift
swiftc -O -o $TMPDIR/reckon-parity $TMPDIR/reckon-parity.swift && $TMPDIR/reckon-parity 2026-10-08
```

If that test fails, the two platforms have silently started disagreeing.

## Layout

| Path | What's in it |
|---|---|
| `model/` | Operations, problem generation, the game engine, the save blob and its store. No Android UI types except the state Compose observes. |
| `ui/theme/` | The palette and typography, mirrored from `ios/Shared/Palette.swift`. |
| `ui/components/` | Keypad, mode cards, operation rows, the pace chart (drawn with `Canvas`). |
| `ui/screens/` | Home, Game, Result. |
| `widget/` | The home-screen widget, a Glance `GlanceAppWidget`. |
| `parity/` | The Swift harness above. Not part of the Android build. |

## Differences from iOS

- Swift's rounded system font has no Android equivalent, so the type scale uses
  the platform sans with tabular figures (`fontFeatureSettings "tnum"`). Sizes,
  weights and tracking match.
- `minimumScaleFactor(0.55)` becomes stepped font sizes picked by question
  length.
- Swift Charts' `.monotone` interpolation is hand-rolled (Fritsch–Carlson
  monotone cubic Hermite in `PaceChart`) — a plain spline overshoots on a jagged
  pace series.
- Glance has no gradient background and no path drawing, so the widget's amber
  wash is dropped and its sparkline is bars rather than a line.
- The engine's clock is polled from a frame loop and its post-answer pauses are
  coroutines, replacing `Timer` and `DispatchQueue.main.asyncAfter` — the delays
  themselves are unchanged, since they're part of how a round feels.
