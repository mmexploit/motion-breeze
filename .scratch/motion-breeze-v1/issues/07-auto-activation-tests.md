Status: ready-for-human

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Write automated tests for the Auto-Activation Pipeline — the module with the most complex state logic in the app. Tests cover 9 scenarios: vehicle enter/exit behavior, grace period timing and cancellation, auto-stop lock, confirm-before-start, and manual stop behavior.

Use JUnit 4 (already in `build.gradle.kts`) with Mockito for mocking Android framework classes. Consider Robolectric for tests needing a real `Context`.

## Acceptance criteria

9 passing test cases:

- [ ] **Vehicle enter starts overlay**: when auto-activate is enabled and overlay is not running, a vehicle enter event dispatches a start intent
- [ ] **Vehicle enter does not start when auto-activate is disabled**: even if a vehicle is detected, no start intent is sent
- [ ] **Vehicle enter does not start when already running**: prevents duplicate start commands
- [ ] **Vehicle exit triggers grace period**: overlay is not stopped immediately — a 2-minute timer is scheduled
- [ ] **Grace period cancelled on re-enter**: a vehicle enter event during the grace period cancels the pending stop
- [ ] **Grace period completes and stops**: after 2 minutes without re-enter, the overlay is stopped
- [ ] **Auto-Stop Lock prevents auto-stop**: when `autoStopLock` is true, vehicle exit events are ignored
- [ ] **Confirm-before-start sends notification**: when enabled, vehicle enter shows a notification instead of starting directly
- [ ] **Manual stop does not disable auto-activate**: notification "Stop" action stops the overlay but leaves auto-activate on

Tests verify external behavior (intents dispatched, timers scheduled/cancelled), not internal implementation. Use a `TestScheduler` or fake clock to control the grace period timer.

## Blocked by

- [Issue 04: Auto-activation wiring](./04-auto-activation-wiring.md)
- [Issue 05: Grace period + auto-stop lock](./05-grace-period-auto-stop-lock.md)
- [Issue 06: Confirm-before-start](./06-confirm-before-start.md)

## Comments