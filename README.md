# Linkpad

> Turn your Android phone into a wireless keyboard, mouse, and media remote for any Bluetooth host. No companion app on the target device.

[![Platform](https://img.shields.io/badge/platform-Android-green)](https://www.android.com/)
[![Min SDK](https://img.shields.io/badge/minSdk-28-blue)](https://developer.android.com/)
[![Target SDK](https://img.shields.io/badge/targetSdk-34-blue)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](#license)

---

## Overview

**Linkpad** is a Bluetooth HID controller built natively for Android. It pairs with any host that supports Classic Bluetooth — macOS, Windows, Linux, iPadOS, Android TV, and most smart TVs — and shows up as a generic combo keyboard + mouse. The host doesn't need any driver, helper app, or pairing code beyond the standard Bluetooth flow.

Use it as a silent remote for your living-room TV, a backup keyboard when yours dies mid-meeting, or a desk-side trackpad when you want your laptop closed.

---

## Features

- **Touchpad** — laptop-class trackpad with 1-finger move, 2-finger scroll (vertical + horizontal AC Pan), 2-finger tap right-click, tap-to-drag, and a precision right-edge scroll strip.
- **Software keyboard** — relays your typing to the host in real time. Optional Shortcuts row (Cmd / Ctrl + C/V/X/Z/A, Alt+Tab), F-keys, Edit cluster (Backspace / Del / Home / End / PgUp / PgDn / Insert), and Arrow cluster.
- **Air mouse** — gyroscope-driven cursor with low-pass filter. Configurable sensitivity, axis invert, and per-button visibility.
- **Media remote** — brightness, volume, mute, prev/next track, play/pause, plus 10 s seek for YouTube / Netflix / Spotify / VLC.
- **Multi-host profiles** — save Mac / PC / iPad / TV profiles, each with its own target-OS layout and last-connected device. Switch in one tap.
- **Quick reconnect** — one-tap button on the connection bar reconnects to the active profile's last device without scanning.
- **Themed UI** — light / dark / system theme, smooth tab transitions, premium nav bar.
- **Connect anything** — combined Classic discovery + BLE scan in parallel; bonded devices pinned to top.

---

## Screens

| Touchpad | Keyboard | Air Mouse |
| :-: | :-: | :-: |
| Trackpad surface + edge scroll strip | Live typing relay + chip rows | Gyro cursor visualizer + click row |

| Media | Connect | Settings |
| :-: | :-: | :-: |
| Brightness / volume / transport | Scan + paired history | Profiles, OS, theme, About |

---

## Build

### Android Studio (recommended)

1. **File → Open** and select the project root.
2. When prompted "Gradle Wrapper not found", choose **OK / Use Gradle from gradle-wrapper.properties**.
3. Studio downloads everything. Hit **Run**.

### Terminal

```sh
brew install gradle
gradle wrapper --gradle-version 8.6
./gradlew assembleDebug
./gradlew installDebug   # installs to a USB-connected device
```

### Variants

| Command | Output |
| --- | --- |
| `./gradlew assembleDebug` | `app-debug.apk` (`com.btremote.app.debug` suffix) |
| `./gradlew assembleRelease` | R8-shrunk signed release APK |
| `./gradlew bundleRelease` | AAB for Play Store |
| `./gradlew lint` | Lint report |
| `./gradlew test` | Unit tests |

---

## Pair with a host

1. Open Linkpad → **Connect** tab → tap **Scan**.
2. Pick the device from the list (bonded devices appear first).
3. On the host, accept the pairing request (Linkpad shows up as a combo keyboard + mouse named **Linkpad**).
4. Done. Switch back to Touchpad / Keyboard / Media — anything you do is mirrored to the host instantly.

After the first pair, the active profile remembers the device. Tap **QUICK** on the connection bar next launch — no scan needed.

### Tested hosts

- macOS 12+
- Windows 10 / 11
- Linux (BlueZ 5.50+)
- iPadOS / iOS 14+
- Android TV / Google TV
- Most smart TVs (LG, Samsung) accept it as a BT keyboard

> **Not supported:** Android phones as the *host*. Stock and skinned Android (MIUI / One UI / ColorOS) filters out HID-Combo from another phone. Not fixable app-side.

---

## Tech stack

- **Kotlin** + **Jetpack Compose** (no XML layouts)
- **Hilt** dependency injection
- **DataStore Preferences** (split across app prefs / paired devices / host profiles)
- **BluetoothHidDevice** API for HID reports
- **MVVM** with one ViewModel per screen
- **Foreground Service** owns the HID profile proxy + device connection lifecycle
- Min SDK 28 · Target SDK 34

---

## Architecture

```
app/
├── bluetooth/         HID descriptor, report sender, service, scan manager
├── data/              Preferences, paired-device cache, host profiles
├── sensor/            Gyroscope reader for air mouse
└── ui/
    ├── components/    Connection bar, mouse buttons, sliders, toggles
    ├── navigation/    Compose nav graph
    ├── screens/       Touchpad / Keyboard / AirMouse / Media / Connect / Settings
    └── theme/         Material3 color, typography
```

The HID descriptor is a byte-exact combo (Boot Keyboard + 5-button mouse with AC Pan + Consumer control). Mouse polling runs at ~125 Hz to match commercial HID devices. Keyboard input uses 6-key rollover with mutex-guarded slot allocation.

---

## Privacy

Linkpad never connects to the internet. All Bluetooth traffic is local. Only data stored:

- A list of recently paired devices (name + MAC), kept on-device.
- Your settings (speeds, toggles, theme).
- Host profiles you create.

Nothing leaves your phone.

---

## Roadmap

- Custom keyboard layouts (AZERTY, QWERTZ, Dvorak)
- Macro recorder
- Lock-screen widget for play / pause / skip
- Material You dynamic color
- Optional encrypted cloud sync of profiles

---

## Contributing

Issues and pull requests welcome. Before submitting:

1. Run `./gradlew lint` and `./gradlew test`.
2. Keep diffs focused — one feature or fix per PR.
3. If you touch the HID descriptor, **re-pair every test host** before retesting (hosts cache the descriptor at bond time).

---

## License

MIT — see [LICENSE](LICENSE).

---

## Author

**Devdas Kumar** · [@Devdas-gupta](https://github.com/Devdas-gupta)

If Linkpad saved you a Bluetooth dongle, a star on the repo is appreciated.
