# Install

Two ways: take a build from the release, or build it from source.

## From the release

[v1.0](https://github.com/saxill/reckon/releases/tag/v1.0) carries two files.

### Android — `reckon-android-debug.apk`

A debug-signed APK, about 18 MB. It installs on any Android phone (minSdk 26):
either

```bash
adb install reckon-android-debug.apk
```

or copy it to the phone and tap it (you may need to allow installs from your file
manager or browser). Nothing else is needed — no account, no pairing.

### iOS — `Reckon.ipa`

This one is restricted, so read this before downloading it.

The IPA is **development-signed** under Apple team `K75VPCXM64`. It installs
**only on the single iPhone registered to that team** — every other device will
refuse it. Its provisioning profile **expires 2026-10-13**; after that date the
app stops launching and must be rebuilt and reinstalled. Installing a
development-signed IPA needs the phone registered to the team plus a tool that
can push it (Xcode's Devices window or Apple Configurator).

Building from source, below, is the more flexible route: it works on any iPhone
you can sign for, so long as you change the team and bundle id to your own.

## From source

### iOS

Xcode 16+ and iOS 17+.

```bash
open ios/Reckon.xcodeproj
```

Pick an iPhone simulator and press ⌘R, or see [`../ios/README.md`](../ios/README.md)
for installing to a real phone (`./Scripts/install-on-device.sh`), the signing
team, the widget's App Group, and the free-Apple-ID 7-day expiry.

### Android

JDK 17 and the Android SDK.

```bash
brew install openjdk@17
```

```bash
cd android
./gradlew installDebug        # build and install onto a connected phone or emulator
./gradlew testDebugUnitTest   # run the 35 unit tests
```

`minSdk` is 26 and `targetSdk` 35. If Gradle cannot find the Android SDK, point
`local.properties` at it:

```properties
sdk.dir=/usr/local/share/android-commandlinetools
```

See [`../android/README.md`](../android/README.md) for the build layout and the
Daily parity harness.

## What you need installed

| Platform | Toolchain |
|---|---|
| iOS | Xcode 16+ |
| Android | JDK 17 (`brew install openjdk@17`) and the Android SDK |
