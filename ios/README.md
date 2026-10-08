# Reckon

Mental-arithmetic drills for iOS. A native SwiftUI rebuild of the single-file
`reckon.html` prototype — same maths, same scoring, a proper iOS interface.

## Run it

```bash
open ~/code/Reckon/ios/Reckon.xcodeproj
```

Pick an iPhone simulator and hit ⌘R. Requires Xcode 16+ / iOS 17+.

### On your own iPhone

The project is set to team `K75VPCXM64` (the personal team on this Mac). Change it
under Signing & Capabilities if you use another, and change the bundle id
(`com.sahilchanna.reckon`) if it's taken. Note the team id is the certificate's
**OU** field, not the code in its name:

```bash
security find-certificate -c "Apple Development" -p | openssl x509 -noout -subject
```

Xcode needs an Apple account signed in for provisioning: **Xcode → Settings →
Accounts** (password and two-factor — only you can do that part).

Then either press ⌘R with your iPhone selected, or:

```bash
./Scripts/install-on-device.sh
```

On a free Apple ID the first launch needs trusting: iPhone → Settings → General →
VPN & Device Management → trust your developer certificate.

Builds signed by a **free** Apple ID expire after 7 days — when the app stops
launching, run the install script again. If provisioning ever rejects the App
Group the widget needs, either join the paid Developer Program or delete the
`com.apple.security.application-groups` entries from both files in `Config/`: the
app carries on unchanged (it falls back to its own storage) and only the widget
loses live numbers.

For screenshots, launch with `-seedDemo` (a DEBUG-only argument, Scheme → Run →
Arguments) to fill the app and widget with plausible history. It writes to the
App Group, so it replaces real scores on that device — debug builds only.

## Modes

| Mode | Rules | Scored on |
|---|---|---|
| Sprint | 60 seconds, each miss adds 2 seconds to the clock | problems solved |
| Marathon | 20 problems, 3 lives | time to finish |
| Daily | 15 problems, seeded from the date, identical for everyone | score + streak |

Operations (+ − × ÷) and difficulty 1–5 are chosen on the home screen; Daily
always uses all four at level 3. Answers submit themselves once you've typed as
many digits as the answer has.

## What it tracks

- **Median seconds per solve** across your last 14 rounds — the headline number,
  with a sparkline where a rising line means you're getting faster.
- **Per-operation speed and accuracy**, so the slowest drill is obvious, plus a
  one-tap sprint against only that operation.
- Personal bests, Daily streak, and the problems you missed in each round.

Everything is local: a small JSON blob in `UserDefaults`. No network code, no
accounts, no analytics.

## Keyboard and iPad

With a hardware keyboard (iPad, Stage Manager, a Mac running the iPad app):

| Key | Does |
|---|---|
| `0`–`9` | type the answer — it still submits itself once you've typed enough digits |
| `Return` | submit what's typed; on the home screen it starts a Sprint; on the result screen it plays again |
| `Delete` | backspace |
| `Esc` | end the round, or go home from the result screen |

Key handling lives in one place, `RootView.handleKey`, so no screen fights over
focus. Focus is re-asserted when the route changes and when the app becomes
active, otherwise the keyboard goes dead after a trip to the background.

On iPad the app is universal and rotates. Cards and the keypad stay at a
phone-width column (`Layout.maxContent`) centred on the screen rather than
stretching, and in landscape the play screen splits: the problem on the left,
the keypad on the right, so the keys stay in reach.

## Home Screen widget

Small and medium sizes, showing median solve time, daily streak, whether today's
Daily is done, a sparkline of recent rounds, and your bests. Tapping it opens
straight into a Sprint via `reckon://sprint`.

Add it: long-press the Home Screen → Edit → Add Widget → search "Reckon".

The widget is a separate process, so the app and the extension share their data
through the App Group `group.com.sahilchanna.reckon` (declared in
`Config/*.entitlements`, read and written by `ReckonDefaults` in
`Shared/SaveData.swift`). Saving a round calls `WidgetCenter.reloadAllTimelines()`;
otherwise the timeline sits still until just after midnight, when the Daily resets.

**On your own device** you'll need that App Group on your developer account —
Xcode's automatic signing offers to create it when you select your team. Change
the group name in both entitlements files and in `ReckonDefaults.suiteName` if you
use a different bundle prefix.

## Layout

```
Shared/                  compiled into both the app and the widget
  Operation.swift        the four drills + difficulty ranges
  SaveData.swift         the save blob, App Group storage, derived stats
  Palette.swift          colours (dark + light) and number formatting
ReckonWidget/
  ReckonWidgetBundle.swift
  ReckonWidget.swift     timeline provider, small and medium layouts
Config/                  Info.plists and App Group entitlements
Reckon/
  ReckonApp.swift        app entry, injects the store
  Theme.swift            fonts, card chrome, press behaviour
  Haptics.swift          prepared feedback generators
  Model/
    Problem.swift        problem generation, mulberry32 seeded RNG for Daily
    GameMode.swift       mode rules and the round result type
    GameEngine.swift     one round: timing, input, scoring, lives
    Store.swift          round recording, personal bests, streak
  Views/
    RootView.swift       home / play / result routing
    HomeView.swift       hero, modes, per-operation profile, settings
    GameView.swift       HUD, question, keypad
    ResultView.swift     score, stat tiles, misses
  Components/            keypad, mode card, chips, chart, profile row
```

## Differences from the web version

- Haptics on every keypress, solve and miss.
- Swift Charts sparkline in place of the CSS bars, and the missed answer is
  revealed on screen the moment you get one wrong.
- Native dark/light palettes rather than a dark-only page with a light fallback.
- Keypad sized for thumbs (64pt rows), spring press states, and shake + colour
  feedback on the answer.
- The same operation never appears twice in a row when more than one is enabled.
