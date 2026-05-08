Status: needs-triage

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

When `confirmBeforeStart` is enabled in settings, vehicle enter events send a high-priority notification instead of directly starting the overlay. Tapping the notification starts the overlay.

This is partially implemented in `ActivityTransitionReceiver.showConfirmationNotification()` — the notification is already built with a "Vehicle detected — tap to enable motion cues" message and a `PendingIntent`. This issue verifies and fixes the wiring so the `PendingIntent` actually starts the overlay when tapped, and ensures the flow works end-to-end.

## Acceptance criteria

- [ ] When `confirmBeforeStart` is true and a vehicle enter event arrives, the `ActivityTransitionReceiver` sends a high-priority notification instead of directly starting the overlay
- [ ] The notification says "Vehicle detected" with "Tap to enable motion cues"
- [ ] Tapping the notification starts the overlay (`ACTION_START_OVERLAY`)
- [ ] The notification auto-cancels after being tapped
- [ ] When `confirmBeforeStart` is false (default), vehicle enter immediately starts the overlay (existing behavior from Issue 04)
- [ ] The confirm notification only appears when auto-activate is enabled

## Blocked by

- [Issue 04: Auto-activation wiring](./04-auto-activation-wiring.md) (needs vehicle enter events being received)

## Comments