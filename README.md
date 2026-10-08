# Reckon

Mental-arithmetic drills. Timed rounds across four operations, with a median
solve time it tracks over your last 14 rounds as the headline number.

- **Sprint** — 60 seconds, each miss adds 2 seconds to the clock
- **Marathon** — 20 problems, 3 lives
- **Daily** — 15 problems, seeded from the date, identical for everyone

Everything is local. No network, no accounts, no analytics.

## Screenshots

| Light | Dark |
|---|---|
| ![Home, light](docs/screenshots/home.png) | ![Home, dark](docs/screenshots/home-dark.png) |

## Platforms

| | Path | Status |
|---|---|---|
| iOS | [`ios/`](ios/) | SwiftUI, iOS 17+, home-screen widget |
| Android | [`android/`](android/) | Kotlin + Jetpack Compose, Glance widget — done, full parity |

## Download

[v1.0](https://github.com/saxill/reckon/releases/tag/v1.0) has `reckon-android-debug.apk`
(18 MB), a debug-signed APK that installs on any Android phone, and `Reckon.ipa`
(1.1 MB), which is development-signed for one registered iPhone and stops
launching when its profile expires on 2026-10-13. See
[`docs/install.md`](docs/install.md) for the details and the build-from-source route.

## iOS

```bash
open ios/Reckon.xcodeproj
```

Pick an iPhone simulator and hit ⌘R. Requires Xcode 16+ / iOS 17+. See
[`ios/README.md`](ios/README.md) for device installs, the widget, keyboard
handling and the module layout.

## Android

```bash
cd android
./gradlew installDebug        # build and install onto a connected phone or emulator
./gradlew testDebugUnitTest   # run the 35 unit tests
```

Needs JDK 17 (`brew install openjdk@17`) and an Android SDK; `minSdk` 26,
`targetSdk` 35. See [`android/README.md`](android/README.md) for the build
layout, the Daily's cross-platform parity story, and the deliberate differences
from iOS.

## Documentation

See [`docs/README.md`](docs/README.md) for the documentation index — the
per-platform READMEs, [`docs/architecture.md`](docs/architecture.md) and
[`docs/install.md`](docs/install.md).
