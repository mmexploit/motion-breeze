# ADR 0001: Full Kotlin with Native Service (not React Native/Expo)

## Status

Accepted

## Context

Motion Breeze started as an Expo/React Native project. The core feature — system-wide motion cue dots rendered over other apps — requires an Android foreground Service with `SYSTEM_ALERT_WINDOW` permission. This cannot be implemented in React Native.

A hybrid architecture (Expo UI + native Service) was considered. It would require:
- A React Native settings screen communicating with a Kotlin Service via bridge or IPC
- Native module wrappers for SharedPreferences, overlay lifecycle, and sensor data
- Expo development builds (cannot use Expo Go) for any native change
- Significant bridge complexity for real-time sensor data that must operate at 60fps

## Decision

Build the entire app in Kotlin with Jetpack Compose. The overlay Service, sensor pipeline, Activity Recognition receiver, and settings UI are all native.

## Consequences

- **Positive**: No RN-native bridge latency for sensor data. No expo-dev-client rebuild cycle. Smaller APK. Simpler architecture — one language, one build system, direct SharedPreferences access.
- **Negative**: Android-only. No cross-platform potential. Team must be comfortable with Kotlin/Compose.
- **Neutral**: The UI surface is small (onboarding, settings, home toggle). Compose handles this well. The overlay Service is the app's core — it was always going to be Kotlin.