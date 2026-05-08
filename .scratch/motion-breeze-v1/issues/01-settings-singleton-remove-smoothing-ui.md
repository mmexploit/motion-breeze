Status: ready-for-human

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Hoist `SettingsRepository` to `Application`-level singleton and remove the `smoothingMs` slider from the Settings screen.

Currently `SettingsRepository` is created in `NavGraph` via a `remember {}` block tied to composition lifecycle — it should be a singleton. The `smoothingMs` field remains in `DotSettings` (default 75ms) but no slider appears in `SettingsScreen`, since smoothing is a developer concept users don't need to control.

## Acceptance criteria

- [ ] `SettingsRepository` is created once in `MotionBreezeApp` and accessed as a singleton (via companion `getInstance(app)` or manual DI)
- [ ] `NavGraph` no longer creates its own `SettingsRepository` — receives the singleton
- [ ] `SettingsScreen` no longer shows a `smoothingMs` slider
- [ ] `smoothingMs` field remains in `DotSettings` with default 75 and is still read/written by SharedPreferences (used by the overlay service)
- [ ] All existing settings (dot count, size, sensitivity, opacity, auto-activate, confirm-before-start) still work

## Blocked by

None — can start immediately.

## Comments