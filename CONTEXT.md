# Motion Breeze

Android app that mimics Apple's Vehicle Motion Cues feature — animated dots on screen edges that move opposite to vehicle motion, reducing motion sickness for passengers using their phone in a moving vehicle.

## Domain Glossary

| Term | Definition |
|------|-----------|
| **Motion Cue** | A semi-transparent white dot with a subtle dark outline, rendered on left and right screen edges, that drifts opposite to detected vehicle acceleration/rotation, providing peripheral visual reference to reduce motion sickness |
| **Overlay** | The system-wide transparent layer rendered via `SYSTEM_ALERT_WINDOW` permission that displays motion cues on top of any app. Always locked to portrait orientation regardless of device rotation. |
| **In-App Mode** | Fallback mode where motion cues are only visible within the Motion Breeze app itself (when overlay permission is denied) |
| **Parallax Drift** | Motion cue rendering technique where each dot moves at a different speed based on its `depthFactor`, creating a sense of depth. Dots closer to screen center move faster |
| **Lateral Motion** | Horizontal acceleration/rotation from vehicle turning — primary driver of horizontal dot movement |
| **Longitudinal Motion** | Forward/backward acceleration from braking/accelerating — drives vertical dot movement |
| **Depth Factor** | Per-dot multiplier (0.5–1.0) that scales how much a dot responds to motion. Dots at screen-top/bottom edges have higher depth factors |
| **Smoothing** | Low-pass filter applied to raw sensor data to prevent jitter. Default: 75ms. Controls how quickly dots respond to motion changes |
| **Sensitivity** | User-adjustable multiplier (0.1x–2.0x) that scales how strongly dots react to detected motion |
| **Auto-Activation** | Feature that uses Google's Activity Recognition API to detect when user is in a vehicle and automatically start the overlay |
| **Confirm-Before-Start** | Setting that, when enabled, sends a notification asking user confirmation before auto-starting the overlay |
| **Grace Period** | 2-minute window after Activity Recognition detects the user has exited a vehicle, during which the overlay keeps running. Cancels if a "vehicle enter" event fires during the window |
| **Auto-Stop Lock** | Setting that prevents the overlay from being auto-stopped when a vehicle exit is detected. User retains full manual control of start/stop |
| **Overlay Service** | Android foreground `Service` that renders the motion cue overlay window and processes sensor data natively |
| **Activity Transition Receiver** | `BroadcastReceiver` that listens for vehicle enter/exit transitions from Google Play Services Activity Recognition |

## Architecture

- **Language**: Kotlin with Jetpack Compose
- **Sensor Pipeline**: Native Android `SensorManager` → low-pass filter → dot offset calculation → Canvas redraw (all in `OverlayService`)
- **Settings Communication**: `SharedPreferences` with `OnSharedPreferenceChangeListener` — Expo/React Native is not involved; the Compose settings screen and native Service both read/write the same prefs file
- **Auto-Activation**: Google Play Services `ActivityRecognition` API → `ActivityTransitionReceiver` → starts/stops `OverlayService`
- **Permissions**: `SYSTEM_ALERT_WINDOW` (overlay), `ACTIVITY_RECOGNITION` (auto-activate), `FOREGROUND_SERVICE` (overlay service), `POST_NOTIFICATIONS` (foreground service notification), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (prevent service kills on aggressive OEMs)

## Key Decisions

1. **Full Kotlin (not React Native)**: The overlay must be a native Android Service. A hybrid RN+Kotlin architecture would add bridge complexity without benefit since the feature is Android-only by nature.
2. **SharedPreferences over IPC**: Settings flow from Compose UI → SharedPreferences → Service reads via change listener. No IPC latency, live updates.
3. **Sensor-driven in native**: All sensor reading and dot rendering happens in Kotlin. No JS bridge latency for real-time motion data.
4. **Activity Recognition + manual toggle**: Default auto-activate, optional confirm-before-start, always a manual toggle available.
5. **Graceful degradation**: Without overlay permission, the app still functions in in-app mode with motion cues visible within the app itself.
6. **In-app mode via MotionCueActivity**: Full-screen translucent activity with dots rendered by the OverlayService. Not as good as system-wide overlay, but functional for passive use.
7. **Automatic overlay upgrade**: If overlay permission is granted while in-app mode is active, seamlessly switch to system-wide overlay without user intervention.
8. **Grace period on auto-stop**: 2-minute grace period after vehicle exit detection, cancelled by a re-enter event. Optional Auto-Stop Lock setting for full manual control.
9. **Screen-off sensor pause**: When the screen turns off, sensor listeners are unregistered to save battery. Re-registered on screen-on with negligible resume delay. Foreground service keeps running.
10. **Battery optimization prompt on first start**: Battery optimization exemption is requested contextually when the user first starts the overlay, not during onboarding.