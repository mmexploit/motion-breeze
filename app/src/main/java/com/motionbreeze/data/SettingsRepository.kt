package com.motionbreeze.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DotSettings(
    val dotsPerSide: Int = 4,
    val dotSizeDp: Int = 8,
    val sensitivity: Float = 1.0f,
    val minOpacity: Float = 0.2f,
    val maxOpacity: Float = 0.6f,
    val smoothingMs: Int = 75,
)

data class AutoActivateSettings(
    val autoActivate: Boolean = true,
    val confirmBeforeStart: Boolean = false,
)

data class AppSettings(
    val dots: DotSettings = DotSettings(),
    val autoActivate: AutoActivateSettings = AutoActivateSettings(),
    val hasCompletedOnboarding: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasActivityRecognitionPermission: Boolean = false,
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("motion_breeze_settings", Context.MODE_PRIVATE)

    val settingsFlow: Flow<AppSettings> = prefs.changesFlow().map { readSettings() }

    fun readSettings(): AppSettings {
        return AppSettings(
            dots = DotSettings(
                dotsPerSide = prefs.getInt(KEY_DOTS_PER_SIDE, 4),
                dotSizeDp = prefs.getInt(KEY_DOT_SIZE_DP, 8),
                sensitivity = prefs.getFloat(KEY_SENSITIVITY, 1.0f),
                minOpacity = prefs.getFloat(KEY_MIN_OPACITY, 0.2f),
                maxOpacity = prefs.getFloat(KEY_MAX_OPACITY, 0.6f),
                smoothingMs = prefs.getInt(KEY_SMOOTHING_MS, 75),
            ),
            autoActivate = AutoActivateSettings(
                autoActivate = prefs.getBoolean(KEY_AUTO_ACTIVATE, true),
                confirmBeforeStart = prefs.getBoolean(KEY_CONFIRM_BEFORE_START, false),
            ),
            hasCompletedOnboarding = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
            hasOverlayPermission = prefs.getBoolean(KEY_HAS_OVERLAY_PERMISSION, false),
            hasActivityRecognitionPermission = prefs.getBoolean(KEY_HAS_ACTIVITY_RECOGNITION, false),
        )
    }

    fun updateDotSettings(dots: DotSettings) = prefs.edit().apply {
        putInt(KEY_DOTS_PER_SIDE, dots.dotsPerSide)
        putInt(KEY_DOT_SIZE_DP, dots.dotSizeDp)
        putFloat(KEY_SENSITIVITY, dots.sensitivity)
        putFloat(KEY_MIN_OPACITY, dots.minOpacity)
        putFloat(KEY_MAX_OPACITY, dots.maxOpacity)
        putInt(KEY_SMOOTHING_MS, dots.smoothingMs)
    }.apply()

    fun updateAutoActivate(settings: AutoActivateSettings) = prefs.edit().apply {
        putBoolean(KEY_AUTO_ACTIVATE, settings.autoActivate)
        putBoolean(KEY_CONFIRM_BEFORE_START, settings.confirmBeforeStart)
    }.apply()

    fun setOnboardingComplete() = prefs.edit()
        .putBoolean(KEY_ONBOARDING_COMPLETE, true)
        .apply()

    fun setHasOverlayPermission(has: Boolean) = prefs.edit()
        .putBoolean(KEY_HAS_OVERLAY_PERMISSION, has)
        .apply()

    fun setHasActivityRecognitionPermission(has: Boolean) = prefs.edit()
        .putBoolean(KEY_HAS_ACTIVITY_RECOGNITION, has)
        .apply()

    fun writeSettingsForOverlay(settings: AppSettings) = prefs.edit().apply {
        putInt(KEY_DOTS_PER_SIDE, settings.dots.dotsPerSide)
        putInt(KEY_DOT_SIZE_DP, settings.dots.dotSizeDp)
        putFloat(KEY_SENSITIVITY, settings.dots.sensitivity)
        putFloat(KEY_MIN_OPACITY, settings.dots.minOpacity)
        putFloat(KEY_MAX_OPACITY, settings.dots.maxOpacity)
        putInt(KEY_SMOOTHING_MS, settings.dots.smoothingMs)
    }.apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun SharedPreferences.changesFlow(): Flow<Unit> =
        kotlinx.coroutines.flow.callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(Unit)
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(Unit)
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    companion object {
        private const val KEY_DOTS_PER_SIDE = "dots_per_side"
        private const val KEY_DOT_SIZE_DP = "dot_size_dp"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_MIN_OPACITY = "min_opacity"
        private const val KEY_MAX_OPACITY = "max_opacity"
        private const val KEY_SMOOTHING_MS = "smoothing_ms"
        private const val KEY_AUTO_ACTIVATE = "auto_activate"
        private const val KEY_CONFIRM_BEFORE_START = "confirm_before_start"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_HAS_OVERLAY_PERMISSION = "has_overlay_permission"
        private const val KEY_HAS_ACTIVITY_RECOGNITION = "has_activity_recognition"
    }
}