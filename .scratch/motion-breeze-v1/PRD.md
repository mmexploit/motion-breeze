# PRD: Motion Breeze v1

## Problem Statement

Passengers using their phones in moving vehicles often experience motion sickness. Apple's Vehicle Motion Cues feature addresses this on iOS, but Android has no equivalent. Motion Breeze brings this capability to Android — animated dots on screen edges that drift opposite to the vehicle's motion, giving the peripheral visual reference needed to reduce nausea. Getting this feature right requires a native Android overlay Service, real-time sensor processing, vehicle detection for auto-activation, and graceful handling of permissions and degradation paths.

## Solution

Motion Breeze is a Kotlin/Jetpack Compose Android app that displays motion cues as semi-transparent white dots with dark outlines on left and right screen edges. The overlay runs system-wide via `SYSTEM_ALERT_WINDOW`, responds to accelerometer and gyroscope data through a low-pass filter with parallax drift, and can auto-activate via Google Play Services Activity Recognition when a vehicle is detected. Users who deny overlay permission get a graceful fallback in a contained demo activity. Settings are configurable (dot count, size, sensitivity, opacity range, auto-activation), changes apply live via SharedPreferences, and the entire pipeline runs natively with no cross-process latency.

## User Stories

1. As a passenger in a moving vehicle, I want to see motion dots on my screen edges that move opposite to the car's motion, so that my peripheral vision has a stable reference point and I feel less nauseous.
2. As a new user, I want to be walked through granting overlay and activity recognition permissions one at a time, so that I understand why each permission is needed before granting it.
3. As a new user who denies the overlay permission, I want to still try motion cues within the app itself, so that I can experience whether the feature helps me before committing to the system permission.
4. As a new user who initially denied overlay permission, I want the app to prompt me to enable it from the home screen whenever I try to start the overlay, so that I have a clear path to the full experience.
5. As a user, I want the overlay to start automatically when my phone detects I'm in a vehicle, so that I don't have to remember to toggle it on every ride.
6. As a user who finds auto-activation annoying, I want to disable it in settings, so that I retain full manual control of when the overlay starts and stops.
7. As a user who wants auto-start but not unwanted starts, I want a "confirm before starting" option that sends me a notification to tap, so that I can approve each ride before the overlay appears.
8. As a user, I want to tap "Stop" on the notification when I don't need motion cues right now, so that the overlay goes away without disabling auto-activation for future rides.
9. As a user who has auto-activation on, I want the overlay to stay running for 2 minutes after my phone thinks I've left the vehicle, so that it doesn't flicker on and off during stop-and-go traffic or brief stops.
10. As a user, I want an "Auto-Stop Lock" setting that prevents the overlay from being auto-stopped entirely, so that I have full manual control over when to stop it.
11. As a user configuring dots, I want to adjust how many dots appear per side (1–10) so that I can find the density that works for me.
12. As a user configuring dots, I want to adjust dot size (4–20dp), so that I can make them more or less prominent.
13. As a user configuring dots, I want to adjust sensitivity (0.1x–2.0x), so that I can control how strongly dots react to vehicle motion.
14. As a user configuring dots, I want to adjust minimum and maximum opacity, so that dots are visible when moving but unobtrusive when still.
15. As a user running the overlay while changing settings, I want my changes to apply instantly without the overlay flickering, so that I can fine-tune in real time.
16. As a user whose phone screen turns off, I want the overlay to pause its sensor processing to save battery, so that my battery isn't drained while I'm not even looking at the screen.
17. As a user whose phone screen turns back on, I want the overlay to resume instantly, so that I don't have to wait for dots to become active again.
18. As a user, I want the overlay to stay locked to portrait orientation, so that dots are always on the physical left/right edges regardless of how I hold the phone.
19. As a user running the overlay for the first time, I want to be prompted to exempt Motion Breeze from battery optimization, so that my phone's manufacturer doesn't kill the overlay service.
20. As a user who starts the overlay in in-app mode, I want to see a live demo of the motion cues with sensor data visible, so that I can evaluate whether the feature works for me.
21. As a user who started in in-app mode and then grants overlay permission, I want the app to seamlessly upgrade to system-wide mode, so that I don't have to restart anything manually.
22. As a user, I want the overlay notification to show "Motion Breeze is active" with a "Stop" button, so that I can stop the overlay from anywhere without opening the app.
23. As a user, I want the dots to be visible on both light and dark backgrounds, so that motion cues work regardless of what app I'm using.
24. As a user, I want the dots on both lateral (left/right) and vertical edges that respond to both turning and braking, so that I get reference points for all directions of vehicle motion.
25. As a user whose vehicle detection was a false positive (e.g. on a train), I want to be able to manually stop the overlay and have it respect my decision until the next vehicle detection, so that I'm not fighting the app.

## Implementation Decisions

### Module 1: OverlayService Core

Enhance the existing `OverlayService` with the following capabilities:

- **Screen-off sensor pause**: Register a `BroadcastReceiver` for `ACTION_SCREEN_OFF` and `ACTION_SCREEN_ON` within the Service. Unregister sensor listeners on screen off, re-register on screen on. The foreground service keeps running — only sensors pause. A `CoroutineScope` or `Handler` in the Service manages the receiver lifecycle.

- **Portrait lock**: Set the overlay `WindowManager.LayoutParams` screen orientation toportrait. Use `SCREEN_ORIENTATION_NOSENSOR` on the overlay window.

- **Update-in-place settings**: Replace the current approach of tearing down and recreating the entire overlay view on settings changes. Only recreate when `dotsPerSide` changes (since dot count requires adding/removing views). For `dotSizeDp`, `sensitivity`, `minOpacity`, `maxOpacity` changes, call existing setter methods on `DotView` instances directly. The `OnSharedPreferenceChangeListener` in the Service should branch on which key changed.

- **Reactive service state**: Expose `OverlayService.isRunning` as a `StateFlow<Boolean>` via a companion object. `HomeScreen` collects this flow to reactively update the start/stop button state. This also fixes the issue where auto-activation starts the overlay but the home screen doesn't reflect it.

- **Smoothing algorithm**: Replace the current `alpha = 1f - (smoothingMs / 1000f)` with the correct exponential time-constant formula: `alpha = 1 - exp(-dt / tau)` where `tau = smoothingMs / 1000.0` and `dt` is derived from the sensor event timestamp delta. This is hidden from users (no UI slider).

### Module 2: Auto-Activation Pipeline

Wire the existing `MotionRecognitionManager` and `ActivityTransitionReceiver` into the app lifecycle:

- **Wire MotionRecognitionManager**: Call `requestActivityUpdates()` when: (1) the user enables auto-activate in settings, or (2) the overlay service starts and auto-activate is enabled. Call `removeActivityUpdates()` when auto-activate is disabled or the service stops. The SettingsScreen auto-activate toggle and OverlayService are the two call sites.

- **Grace period**: When `ActivityTransitionReceiver` receives a vehicle exit event, do not immediately stop the overlay. Instead, schedule a 2-minute delayed `ACTION_STOP` intent via `AlarmManager` or `Handler`. If a vehicle enter event arrives before the 2 minutes elapse, cancel the pending stop. The grace period only applies when auto-activation is enabled and the overlay is running.

- **Auto-Stop Lock**: Add `autoStopLock: Boolean` to `AutoActivateSettings` (default `false`). When enabled, the `ActivityTransitionReceiver` ignores vehicle exit events entirely — the overlay can only be stopped manually or via the notification "Stop" button. Add this toggle to `SettingsScreen` under the Auto-Activation section, visible only when auto-activate is on.

- **Confirm-before-start notification**: Already implemented in `ActivityTransitionReceiver.showConfirmationNotification()`. Ensure it fires when `confirmBeforeStart` is `true` and the user taps the notification to start the overlay.

- **Notification "Stop" does not disable auto-activate**: When the user taps "Stop" on the foreground notification, the overlay stops but auto-activate remains on. The next vehicle enter event will start it again. This is already the current behavior but should have a test asserting it.

### Module 3: In-App Mode

- **MotionCueActivity**: Create a new `Activity` that renders a translucent dark background with a contained area showing live motion dots. This activity hosts a `FrameLayout` that `OverlayService` renders into when running in in-app mode. The activity shows: (1) animated dots responding to real sensor data in a bounded canvas, (2) a "How it works" brief explanation, (3) a prominent "Enable full overlay" CTA linking to system settings for `SYSTEM_ALERT_WINDOW`.

- **OverlayService in-app rendering**: When `ACTION_START_IN_APP` is received, start the foreground service and register sensors as before, but instead of creating a `SYSTEM_ALERT_WINDOW` overlay, bind to `MotionCueActivity`'s `FrameLayout` via a `ServiceConnection`. The service drives dot updates into this container. When the activity is destroyed, the service stops sensors but keeps running (in case the user switches to overlay mode).

- **Automatic overlay upgrade**: In `MotionCueActivity`, register a listener for `SYSTEM_ALERT_WINDOW` permission changes (poll `Settings.canDrawOverlays()` on activity resume). When the permission is detected as granted while running in in-app mode: (1) remove the in-app dot container, (2) create the system overlay window, (3) finish `MotionCueActivity`. This is a seamless transition — the user doesn't need to tap anything.

### Module 4: Home Screen

- **Reactive service state**: Replace the current `remember { mutableStateOf(OverlayService.isRunning) }` with a collection of the `StateFlow<Boolean>` from Module 1. The home screen updates in real time when the overlay starts (via auto-activation) or stops (via notification, grace period, or manual).

- **Battery optimization prompt**: On first overlay start (either overlay or in-app mode), check `PowerManager.isIgnoringBatteryOptimizations()`. If not whitelisted, show a dialog explaining why battery optimization exemption helps reliability, with a button that launches `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Track whether this prompt has been shown before using a `SharedPreferences` flag so it only appears once. This is distinct from onboarding — it's contextual, appearing when the user first experiences the feature.

### Module 5: Settings

- **Auto-Stop Lock toggle**: Add a `Switch` under the Auto-Activation section in `SettingsScreen`, visible only when `autoActivate` is `true`. Label: "Prevent auto-stop". Description: "Overlay will not stop automatically when vehicle exit is detected." This maps to the `autoStopLock` field added in Module 2.

- **Remove smoothing from UI**: The `smoothingMs` field remains in `DotSettings` (defaulting to 75) and is used by the sensor pipeline, but no slider is shown in `SettingsScreen`. This was decided during the design interview — smoothing is a developer concept that shouldn't clutter the user-facing settings.

- **SettingsRepository singleton**: Hoist `SettingsRepository` creation to `MotionBreezeApp` or provide it via dependency injection, rather than creating a new instance per composition in `NavGraph`.

## Testing Decisions

### What makes a good test

Tests should verify external behavior, not implementation details. A good test for the Auto-Activation Pipeline asserts that: given a vehicle enter event with auto-activate enabled, the overlay service starts; given a vehicle exit event, the overlay stops after the grace period (not immediately); given a vehicle re-enter during the grace period, the pending stop is cancelled.

### Module to test: Auto-Activation Pipeline

This module has the most complex state logic in the entire app — it involves event-driven transitions, timed delays, cancellation, and multiple conditional branches. It is the highest-value target for testing.

Tests should cover:

1. **Vehicle enter starts overlay** — when auto-activate is enabled and overlay is not running, a vehicle enter event starts the `OverlayService`.
2. **Vehicle enter does not start overlay when auto-activate is disabled** — even if a vehicle is detected, no start intent is sent.
3. **Vehicle enter does not start overlay when already running** — prevents duplicate start commands.
4. **Vehicle exit triggers grace period** — overlay is not stopped immediately; a 2-minute timer is started.
5. **Grace period cancellation on re-enter** — a vehicle enter event during the grace period cancels the pending stop.
6. **Grace period completes and stops overlay** — after 2 minutes without re-enter, the overlay stops.
7. **Auto-Stop Lock prevents auto-stop** — when `autoStopLock` is true, vehicle exit events are ignored entirely.
8. **Confirm-before-start sends notification** when the setting is enabled, a vehicle enter event shows a notification instead of starting directly.
9. **Manual stop does not disable auto-activate** — the "Stop" action on the notification stops the overlay but leaves auto-activate enabled.

Tests should use a test double for `Context` and `Intent` to verify that the correct service actions are dispatched, and use a test scheduler to control the grace period timer.

### Prior art

No tests currently exist in the codebase. This will be the first test suite. Use JUnit 4 (already in `build.gradle.kts` as a dependency) with Mockito for mocking Android framework classes. Consider Roblectric for running tests that need `Context`.

## Out of Scope

- **App icon and branding design** — using default Android icon for now.
- **Top/bottom edge dots** — only left and right edges, as decided.
- **Edge selection setting** — hardcoded to left/right.
- **Smoothing UI slider** — hidden from users, hardcoded to 75ms.
- **Localization/i18n** — English only for v1.
- **Onboarding redesign** — current two-step onboarding flow is functional and matches the design decisions.
- **International activity recognition** — Activity Recognition API availability varies by region; no region-specific fallbacks.
- **Play Store submission preparation** — privacy policy, content rating, etc.
- **Custom icon for dots** — only white circles with dark outline, no shape options.
- **Dot distribution patterns** — evenly spaced only, no alternative layouts.

## Further Notes

- The existing `OverlayService` is functional for the core overlay + sensor pipeline. The modules above build on top of it — they don't rewrite it.
- `MotionRecognitionManager` is fully implemented but never called. Module 2 wires it into the app lifecycle.
- The `DotView` currently has its package as `com.motionbreeze.service` but lives in the `ui/components/` directory. This should be corrected to match the directory path (`com.motionbreeze.ui.components`).
- The `README.md` still contains Expo boilerplate and needs to be rewritten for the Kotlin project.
- The `NavGraph.kt` creates a new `SettingsRepository` per composition. This should be hoisted to the `Application` class or provided via manual DI.
- ADR-0001 ("Full Kotlin, not React Native") documents the architectural decision to use Kotlin instead of the original Expo/React Native setup.