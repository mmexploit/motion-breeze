Status: ready-for-human

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Create a `MotionCueActivity` — the in-app mode experience when the user denies `SYSTEM_ALERT_WINDOW` permission. This is a translucent full-screen activity with a contained area showing live motion dots (rendered by the OverlayService via binding), a "How it works" explanation, and a prominent "Enable full overlay" CTA.

Currently in-app mode (`ACTION_START_IN_APP`) just launches `MainActivity` — no dots are shown. This issue gives in-app mode a proper visual treatment: the user can see the motion cues working (in a bounded canvas), understand the value, and have a clear path to enable system-wide overlay.

## Acceptance criteria

- [ ] **MotionCueActivity**: New activity declared in `AndroidManifest.xml`. Has a translucent dark background with a bounded area (e.g. a `Card` or `Surface`) showing live motion dots
- [ ] **Service binding**: `OverlayService` in-app mode binds to `MotionCueActivity`'s `FrameLayout` via a `ServiceConnection`. The service drives dot updates into this container. When the activity is destroyed, the service unbinds but keeps running
- [ ] **How it works section**: Brief text explaining what motion cues are and how they help reduce motion sickness
- [ ] **Enable full overlay CTA**: A prominent button that deep-links to `ACTION_MANAGE_OVERLAY_PERMISSION`. After the user grants and returns, the dots should seamlessly upgrade (Issue 09 handles the automatic upgrade — this issue just adds the button)
- [ ] **Live sensor data**: Optional — show accelerometer/gyroscope values in a small debug section below the dots area, so users can see that real sensor data is driving the motion
- [ ] Starting in-app mode from the home screen launches `MotionCueActivity` with dot animation working
- [ ] When `MotionCueActivity` is paused/destroyed, sensors pause but service keeps running

## Blocked by

- [Issue 02: OverlayService core hardening](./02-overlay-service-core-hardening.md) (needs hardened service with reactive state)

## Comments