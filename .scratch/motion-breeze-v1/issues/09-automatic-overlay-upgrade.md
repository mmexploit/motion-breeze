Status: ready-for-human

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

When running in in-app mode (`MotionCueActivity`) and the user grants `SYSTEM_ALERT_WINDOW` permission (via the "Enable full overlay" CTA or system settings), automatically switch to system-wide overlay mode without user intervention.

This is the seamless upgrade: detect the permission grant on activity resume, stop in-app rendering, create the system overlay window, and finish `MotionCueActivity`. The user goes from seeing dots in the app to seeing dots on top of whatever they switch to.

## Acceptance criteria

- [ ] **Permission change detection**: `MotionCueActivity.onResume()` checks `Settings.canDrawOverlays()`. If the permission was just granted while running in this activity, trigger the upgrade
- [ ] **Seamless switch**: The service removes the in-app dot container and creates the system-wide overlay window with `TYPE_APPLICATION_OVERLAY`
- [ ] **Activity finishes**: After the switch, `MotionCueActivity` calls `finish()` — the user is now in whatever app was behind it, with the overlay running
- [ ] **State tracking**: The service persists its running state through the transition (foreground notification stays, sensors stay registered)
- [ ] Only upgrade when the service is in in-app mode — if the service is already in overlay mode, do nothing

## Blocked by

- [Issue 08: In-app demo mode](./08-in-app-demo-mode.md) (needs MotionCueActivity with service binding)

## Comments