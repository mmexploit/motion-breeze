# Motion Breeze

Android app that mimics Apple's Vehicle Motion Cues feature — animated dots on screen edges that drift opposite to vehicle motion, reducing motion sickness for passengers using their phone in a moving vehicle.

## Features

- **System-wide overlay**: Motion cue dots appear on top of any app via an Android foreground service
- **Real-time sensor response**: Accelerometer and gyroscope data drive dot movement with parallax drift
- **Auto-activation**: Google Activity Recognition API detects when you're in a vehicle and automatically starts cues
- **Configurable dots**: Adjust dot count (1–10 per side), size (4–20dp), sensitivity (0.1x–2.0x), and opacity range
- **In-app demo mode**: Try motion cues within the app before granting overlay permission
- **Battery-aware**: Sensors pause when the screen turns off to save battery

## Requirements

- Android 8.0 (API 26) or higher
- Google Play Services (for Activity Recognition auto-activation)

## Build

Open the project in Android Studio, sync Gradle, and run on a device or emulator.

```bash
./gradlew assembleDebug
```

## Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Display motion cue dots over other apps |
| `ACTIVITY_RECOGNITION` | Auto-detect when you're in a vehicle |
| `FOREGROUND_SERVICE` | Keep the overlay running in the background |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent the service from being killed by battery saver |
| `POST_NOTIFICATIONS` | Show foreground service notification |

## Architecture

- **Language**: Kotlin with Jetpack Compose (settings UI) and View system (overlay rendering)
- **Sensor pipeline**: Native `SensorManager` → exponential low-pass filter → parallax dot displacement → Canvas redraw
- **Settings**: `SharedPreferences` with live-update listeners between UI and overlay Service