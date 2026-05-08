Status: needs-triage

## Parent

[PRD: Motion Breeze v1](../PRD.md)

## What to build

Harden the `OverlayService` with five enhancements that make the overlay production-ready: screen-off sensor pause to save battery, portrait lock so dots stay on physical left/right edges, update-in-place settings so adjusting dots doesn't flicker, reactive service state via `StateFlow<Boolean>`, and the correct exponential smoothing algorithm.

Also fix the `DotView` package declaration to match its directory and update edge padding to percentage-based (2% of screen width, min 12dp).

## Acceptance criteria

- [ ] **Screen-off pause**: When screen turns off (broadcast `ACTION_SCREEN_OFF`), sensor listeners are unregistered. When screen turns on (`ACTION_SCREEN_ON`), sensors are re-registered. Foreground service keeps running — only sensors sleep.
- [ ] **Portrait lock**: Overlay window is locked to portrait orientation (use `SCREEN_ORIENTATION_NOSENSOR` on the `WindowManager.LayoutParams`). Dots stay on left/right edges regardless of device rotation.
- [ ] **Update-in-place settings**: Changing `dotSizeDp`, `sensitivity`, `minOpacity`, or `maxOpacity` updates dot properties directly via `DotView` setter methods — no overlay rebuild. Only `dotsPerSide` changes trigger a rebuild (since dot count requires adding/removing views). The `OnSharedPreferenceChangeListener` branches on which key changed.
- [ ] **Reactive service state**: `OverlayService.isRunning` becomes a `StateFlow<Boolean>` in the companion object. The Service updates it on start/stop. `HomeScreen` will consume it in a later issue.
- [ ] **Smoothing algorithm**: Replace `alpha = 1f - (smoothingMs / 1000f)` with `alpha = 1 - exp(-dt / tau)` where `tau = smoothingMs / 1000.0` and `dt` is the sensor event timestamp delta.
- [ ] **DotView package fix**: `MotionDotOverlay.kt` declares `package com.motionbreeze.ui.components` (matching its directory path), not `com.motionbreeze.service`.
- [ ] **Edge padding**: Dot margin from screen edge uses `Math.max(measureWidth * 0.02, 12 * density)` dp equivalent instead of fixed 16px.
- [ ] All acceptance criteria verified by starting the overlay and testing each behavior on device/emulator.

## Blocked by

None — can start immediately. (Issue 01 is independent but nice to merge first.)

## Comments