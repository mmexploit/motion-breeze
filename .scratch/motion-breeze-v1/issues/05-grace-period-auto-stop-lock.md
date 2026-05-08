Status: needs-triage

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Add a 2-minute grace period after vehicle exit before stopping the overlay, with cancellation on re-enter. Add an "Auto-Stop Lock" setting that completely disables auto-stop.

Currently vehicle exit immediately stops the overlay (Issue 04). This causes flickering in stop-and-go traffic or at red lights. The grace period waits 2 minutes before sending the stop command, and cancels if the vehicle re-enters during that window.

The Auto-Stop Lock is a new `autoStopLock: Boolean` field in `AutoActivateSettings` (default false). When enabled, the `ActivityTransitionReceiver` ignores vehicle exit events entirely — the overlay only stops via manual action or the notification "Stop" button. A toggle in `SettingsScreen` (visible when auto-activate is on) controls this.

## Acceptance criteria

- [ ] **Grace period timer**: On vehicle exit, a 2-minute timer starts (via `Handler.postDelayed` with a `Runnable` that sends `ACTION_STOP`). The overlay keeps running during this window.
- [ ] **Cancellation on re-enter**: If a vehicle enter event arrives before the 2 minutes elapse, the pending stop is cancelled via `Handler.removeCallbacks`. The overlay continues running.
- [ ] **Timer completes and stops**: After 2 minutes with no re-enter, the `ACTION_STOP` intent is sent to the overlay service.
- [ ] **`autoStopLock` field**: Added to `AutoActivateSettings` data class (Boolean, default false). Persisted via `SettingsRepository` with a new SharedPreferences key.
- [ ] **Auto-Stop Lock toggle**: A `Switch` in `SettingsScreen` under the Auto-Activation section, visible only when `autoActivate` is `true`. Label: "Prevent auto-stop". Description: "Overlay will not stop automatically when vehicle exit is detected."
- [ ] **Auto-Stop Lock prevents auto-stop**: When `autoStopLock` is true, vehicle exit events are completely ignored — no timer starts, no stop sent. The `ActivityTransitionReceiver` checks this flag before processing exit events.
- [ ] Grace period only applies when auto-activate is enabled and overlay is running.

## Blocked by

- [Issue 04: Auto-activation wiring](./04-auto-activation-wiring.md) (needs basic auto-activation with vehicle enter/exit working)

## Comments