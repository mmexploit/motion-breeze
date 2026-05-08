Status: needs-triage

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Wire the existing `MotionRecognitionManager` and `ActivityTransitionReceiver` into the app lifecycle so auto-activation actually works.

`MotionRecognitionManager` is fully implemented but never called — nobody registers for Activity Recognition transitions. This issue connects the two call sites: (1) the `SettingsScreen` auto-activate toggle, and (2) the `OverlayService` start/stop lifecycle. When auto-activate is on and the service starts, Activity Recognition requests are made. When disabled or the service stops, they're removed.

The `ActivityTransitionReceiver` already handles vehicle enter/exit — this issue ensures it actually receives those events and responds (start overlay on enter, stop on exit — immediate, no grace period yet).

## Acceptance criteria

- [ ] **Settings toggle registers activity recognition**: When the user toggles auto-activate ON in Settings, `MotionRecognitionManager.requestActivityUpdates()` is called. When toggled OFF, `removeActivityUpdates()` is called.
- [ ] **Service start registers activity recognition**: When `OverlayService` starts (any mode) and auto-activate is enabled in settings, `requestActivityUpdates()` is called.
- [ ] **Service stop removes activity recognition**: When `OverlayService` stops, `removeActivityUpdates()` is called.
- [ ] **Vehicle enter starts overlay**: When auto-activate is on and overlay is not running, a vehicle enter event starts `OverlayService` with `ACTION_START_OVERLAY`.
- [ ] **Vehicle exit stops overlay**: When overlay is running, a vehicle exit event sends `ACTION_STOP` to the service (immediate stop — grace period is Issue 05).
- [ ] **Already running is a no-op**: Vehicle enter while overlay is already running does not send another start command.
- [ ] **Auto-activate disabled blocks start**: Vehicle enter when auto-activate is off does nothing.
- [ ] **Notification "Stop" preserves auto-activate**: Tapping "Stop" on the foreground notification stops the overlay but auto-activate remains ON — the next vehicle detection will start it again.

## Blocked by

- [Issue 02: OverlayService core hardening](./02-overlay-service-core-hardening.md) (needs reactive service state for `isRunning` check)

## Comments