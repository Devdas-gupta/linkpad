# BTRemote (Linkpad) — UPDATE NOTES

---

## 🚀 Major Updates in v1.3.0

### 1. 🎮 Game-Style Gyro Mouse (Drift-Free)
- **Game Mode**: Air Mouse now uses `TYPE_GAME_ROTATION_VECTOR` (sensor fusion — gyro + accelerometer) for drift-free, quaternion-based cursor movement — the same technique used in FPS gaming controllers.
- **Dead Zone**: Micro-tremors are filtered out so the cursor stays perfectly still when the phone is held steady.
- **Non-Linear Sensitivity Curve**: Slow tilt = precise movement · Fast tilt = quick movement, just like a gaming mouse DPI curve.
- **Fallback**: Devices without game rotation vector sensor automatically fall back to the classic gyro mode.

### 2. 🌐 Air Mouse Works on All Screens
- Air Mouse is now **global** — the gyro sensor runs app-wide in the background regardless of which tab is active (keyboard, touchpad, media, etc.).
- No need to stay on the Air Mouse tab to use tilt-to-cursor while typing.

### 3. 🔘 Volume Buttons as Mouse Clicks
- Volume buttons can now be assigned to mouse click actions:
  - **Remote** — controls PC/Mac volume via BT HID
  - **System** — controls Android device volume
  - **Vol↑ = Left Click / Vol↓ = Right Click** — click and right-click without touching the screen

### 4. 🎨 Air Mouse Screen Redesign
- Animated pulse status dot that breathes when the sensor is active.
- Polished crosshair with glow cursor, halo effect, and white specular highlight.
- Sensitivity level badge shown on the canvas.
- Cleaner button hierarchy: filled (LEFT/RIGHT), tonal (MIDDLE), outlined (RESET).

---

## 🛠️ Bug Fixes in v1.3.0

- **Dagger Circular Dependency Fix**: Resolved a `DependencyCycle` crash caused by `HidServiceController` and `AirMouseController` injecting each other. Fixed by injecting `HidReportSender` directly.
- **Game Mode Conditional Toggle**: Settings only shows the "Game Mode" toggle if the device actually has the required `TYPE_GAME_ROTATION_VECTOR` sensor.

---

## 🚀 Major Updates in v1.2.0

### 1. ✨ Custom Shortcut Keys & Custom Editor
Users can now define, save, and edit their own shortcut keys directly in the Keyboard tab.
- **Modifiers & Keys**: Supports all combinations of Cmd/Ctrl/Option/Alt/Shift/Win modifiers with letters, numbers, F-keys, and special keys.
- **Platform Specific**: Choose whether a shortcut applies to macOS (⌘), Windows (⊞), or both (✦).
- **Interactive UI**: Includes a live preview of the combo and a clean key selection layout.

### 2. 📱 Accessibility & Touch Targets Overhaul
- **Shortcut Actions Dialog**: Replaced the tiny 26dp floating action bar with a clean, fully accessible modal dialog on long-press. Users can now easily Edit or Delete shortcuts with large, tactile buttons that follow standard Material Design guidelines.
- **Stale Grid Selection Fix**: Solved a critical Compose stale-lambda-capture bug in the key selection grid. Tab, Esc, and other keys now register flawlessly on the first tap.
- **Modern Squarish-Rounded Shape**: Redesigned custom shortcut chips and the **＋** Add button to use a modern squircle style (`RoundedCornerShape(12.dp)`) instead of capsules.

### 3. ⚙️ New Settings & Default Preferences
- **Show/Hide Shortcuts Toggle**: Added a toggle under Settings > Keyboard to allow users to completely hide the "My Shortcuts" row from the Keyboard screen. Default value is now **Disabled**.
- **Disabled Air Mouse Reset by Default**: Adjusted the default setting for showing the Air Mouse Reset button to **Disabled** as requested.

---

## 🛠️ Major Bug Fixes

### 1. Bluetooth & Connection Stability
- **Real-Time State Watcher**: Solved the "Connecting" UI state freeze by separating the profile proxy connect call from synchronous timeout blocks. State changes now update in real-time.
- **Windows HID Stall Fix**: Acknowledged `onSetReport` requests appropriately to prevent the Bluetooth HID pipeline from stalling on Windows hosts.
- **Failures & Auto-Reconnect**: Monitors the connection in the background and automatically triggers a reconnection sequence after 5 consecutive packet failures.

### 2. Controls & Device Responsiveness
- **Touchpad & Mouse Fluidity**: Smoothed out cursor movements via atomic buffer drains to prevent stuttering.
- **TV Mode Navigation**: Fixed TV Back and Home buttons by mapping them to correct Android accessibility usage codes (`AC_BACK` / `AC_HOME`).
- **Direct Input Mode**: Added raw HID forwarding options for physical hardware keyboard inputs.

