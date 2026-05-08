Status: needs-triage

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Make the home screen reactively reflect the overlay's running state (via the `StateFlow<Boolean>` from Issue 02) and show a one-time battery optimization prompt the first time the user starts the overlay.

The home screen's `isOverlayRunning` state currently captures the value at composition time only — it doesn't update when the overlay is started/stopped by auto-activation or the notification "Stop" button. Collect the `StateFlow` reactively so the UI stays in sync.

The battery optimization prompt uses `PowerManager.isIgnoringBatteryOptimizations()` and shows a dialog with an explanation and a "Allow" button that opens system settings. A `SharedPreferences` flag ensures the prompt only appears once.

## Acceptance criteria

- [ ] Home screen observe `OverlayService.runningState` (the `StateFlow<Boolean>` from Issue 02) in a `LaunchedEffect` or `collectAsState()` — the start/stop UI updates in real-time when the service starts/stops externally
- [ ] On first overlay start (overlay or in-app mode), check `PowerManager.isIgnoringBatteryOptimizations()`. If not whitelisted, show a dialog: "Keep Motion Breeze running reliably? → Allow battery optimization" with a button that launches `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- [ ] The dialog only appears once — a `SharedPreferences` flag in `SettingsRepository` tracks whether it's been shown
- [ ] If user already has battery optimization whitelisted, no dialog appears
- [ ] Home screen still shows missing permission card when overlay permission is denied

## Blocked by

- [Issue 02: OverlayService core hardening](./02-overlay-service-core-hardening.md) (needs `StateFlow` on OverlayService)

## Comments