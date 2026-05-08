package com.motionbreeze.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.motionbreeze.MainActivity
import com.motionbreeze.data.SettingsRepository
import com.motionbreeze.ui.components.DotView
import com.motionbreeze.ui.screens.inapp.MotionCueActivity
import com.motionbreeze.util.MotionRecognitionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OverlayService : Service(), SensorEventListener {

    private lateinit var windowManager: WindowManager
    private lateinit var sensorManager: SensorManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private var dotViews: List<DotView> = emptyList()

    private var smoothedAccelX = 0f
    private var smoothedAccelY = 0f
    private var smoothedGyroX = 0f
    private var smoothedGyroY = 0f

    private var isOverlayMode = false
    private var isRunning = false

    private val screenReceiver = ScreenStateReceiver()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        settingsRepository = (application as com.motionbreeze.MotionBreezeApp).settingsRepository
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OVERLAY -> startOverlay()
            ACTION_START_IN_APP -> startInApp()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOverlay() {
        if (isRunning) {
            if (!isOverlayMode) {
                isOverlayMode = true
                createOverlayView()
            }
            return
        }
        isRunning = true
        isOverlayMode = true

        startForeground(NOTIFICATION_ID, createNotification())
        createOverlayView()
        registerScreenReceiver()
        registerSensorListener()
        registerSettingsListener()
        registerActivityRecognitionIfEnabled()

        _runningState.value = true
    }

    private fun startInApp() {
        if (isRunning) return
        isRunning = true
        isOverlayMode = false

        startForeground(NOTIFICATION_ID, createNotification())
        registerScreenReceiver()
        registerSensorListener()
        registerSettingsListener()
        registerActivityRecognitionIfEnabled()

        _runningState.value = true

        val launchIntent = Intent(this, MotionCueActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(launchIntent)
    }

    private fun stop() {
        unregisterScreenReceiver()
        unregisterSensorListener()
        unregisterSettingsListener()
        removeActivityRecognition()
        removeOverlayView()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isRunning = false
        _runningState.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenReceiver()
        unregisterSensorListener()
        unregisterSettingsListener()
        removeActivityRecognition()
        removeOverlayView()
        _runningState.value = false
    }

    private fun createOverlayView() {
        val settings = settingsRepository.readSettings().dots

        val container = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        dotViews = createDots(container, settings)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        }

        windowManager.addView(container, overlayParams)
        overlayView = container
    }

    private fun createDots(container: FrameLayout, dots: com.motionbreeze.data.DotSettings): List<DotView> {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val edgePadding = (screenWidth * 0.02f).coerceAtLeast(12f * density).toInt()

        val dotViewsList = mutableListOf<DotView>()

        for (i in 0 until dots.dotsPerSide) {
            val t = if (dots.dotsPerSide == 1) 0.5f else i.toFloat() / (dots.dotsPerSide - 1)
            val yOffset = (screenHeight * 0.1f + screenHeight * 0.8f * t).toInt()
            val depthFactor = 0.5f + 0.5f * kotlin.math.abs(t - 0.5f) * 2f

            val leftDot = DotView(this, depthFactor).apply {
                setDotSize(dots.dotSizeDp)
                setOpacityRange(dots.minOpacity, dots.maxOpacity)
            }
            val leftParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.START or Gravity.TOP
                leftMargin = edgePadding
                topMargin = yOffset
            }
            container.addView(leftDot, leftParams)
            dotViewsList.add(leftDot)

            val rightDot = DotView(this, depthFactor).apply {
                setDotSize(dots.dotSizeDp)
                setOpacityRange(dots.minOpacity, dots.maxOpacity)
            }
            val rightParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                rightMargin = edgePadding
                topMargin = yOffset
            }
            container.addView(rightDot, rightParams)
            dotViewsList.add(rightDot)
        }

        return dotViewsList
    }

    private fun removeOverlayView() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
        dotViews = emptyList()
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun unregisterScreenReceiver() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
    }

    private fun registerSensorListener() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(this)
    }

    private fun registerSettingsListener() {
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            handleSettingsChange(key)
        }
        settingsRepository.registerListener(prefsListener)
    }

    private fun unregisterSettingsListener() {
        settingsRepository.unregisterListener(prefsListener)
    }

    private fun registerActivityRecognitionIfEnabled() {
        val settings = settingsRepository.readSettings()
        if (settings.autoActivate.autoActivate) {
            MotionRecognitionManager.requestActivityUpdates(this)
        }
    }

    private fun removeActivityRecognition() {
        MotionRecognitionManager.removeActivityUpdates(this)
    }

    private fun handleSettingsChange(key: String?) {
        if (!isOverlayMode || overlayView == null) return

        val newDots = settingsRepository.readSettings().dots

        when {
            key == null || key == "dots_per_side" -> {
                removeOverlayView()
                createOverlayView()
            }
            key == "dot_size_dp" -> {
                for (dot in dotViews) dot.setDotSize(newDots.dotSizeDp)
            }
            key == "min_opacity" || key == "max_opacity" -> {
                for (dot in dotViews) dot.setOpacityRange(newDots.minOpacity, newDots.maxOpacity)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val settings = settingsRepository.readSettings().dots
        val tau = settings.smoothingMs / 1000.0
        val dt = 1.0 / 60.0
        val alpha = (1.0 - kotlin.math.exp(-dt / tau)).toFloat()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                smoothedAccelX = smoothedAccelX + alpha * (event.values[0] - smoothedAccelX)
                smoothedAccelY = smoothedAccelY + alpha * (event.values[1] - smoothedAccelY)
            }
            Sensor.TYPE_GYROSCOPE -> {
                smoothedGyroX = smoothedGyroX + alpha * (event.values[1] - smoothedGyroX)
                smoothedGyroY = smoothedGyroY + alpha * (event.values[0] - smoothedGyroY)
            }
        }

        val lateralMotion = -(smoothedAccelX + smoothedGyroY) * settings.sensitivity
        val longitudinalMotion = -(smoothedAccelY + smoothedGyroX) * settings.sensitivity

        val motionIntensity = kotlin.math.sqrt(
            lateralMotion * lateralMotion + longitudinalMotion * longitudinalMotion
        )
        val intensityClamp = (motionIntensity / 5f).coerceIn(0f, 1f)

        for (dot in dotViews) {
            dot.updateMotion(
                lateralOffset = lateralMotion,
                longitudinalOffset = longitudinalMotion,
                intensity = intensityClamp,
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(getString(com.motionbreeze.R.string.notification_title))
                .setContentText(getString(com.motionbreeze.R.string.notification_text))
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(com.motionbreeze.R.string.notification_stop), stopPendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(getString(com.motionbreeze.R.string.notification_title))
                .setContentText(getString(com.motionbreeze.R.string.notification_text))
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(com.motionbreeze.R.string.notification_stop), stopPendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (isRunning) unregisterSensorListener()
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (isRunning) registerSensorListener()
                }
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "motion_breeze_overlay"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_OVERLAY = "com.motionbreeze.action.START_OVERLAY"
        const val ACTION_START_IN_APP = "com.motionbreeze.action.START_IN_APP"
        const val ACTION_STOP = "com.motionbreeze.action.STOP"

        private val _runningState = MutableStateFlow(false)
        val runningState: StateFlow<Boolean> = _runningState

        val isRunning: Boolean get() = _runningState.value
    }
}