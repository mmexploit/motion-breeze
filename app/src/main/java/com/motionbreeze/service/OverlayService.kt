package com.motionbreeze.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.motionbreeze.MainActivity
import com.motionbreeze.R
import com.motionbreeze.data.SettingsRepository

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

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        settingsRepository = SettingsRepository(this)
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
        if (isRunning) return
        isRunning = true
        isOverlayMode = true

        startForeground(NOTIFICATION_ID, createNotification())

        createOverlayView()
        registerSensorListener()
        registerSettingsListener()

        companionIsRunning = true
    }

    private fun startInApp() {
        if (isRunning) return
        isRunning = true
        isOverlayMode = false

        startForeground(NOTIFICATION_ID, createNotification())

        registerSensorListener()
        registerSettingsListener()

        companionIsRunning = true

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(launchIntent)
    }

    private fun stop() {
        unregisterSensorListener()
        unregisterSettingsListener()
        removeOverlayView()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isRunning = false
        companionIsRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensorListener()
        unregisterSettingsListener()
        removeOverlayView()
        companionIsRunning = false
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
        }

        windowManager.addView(container, overlayParams)
        overlayView = container
    }

    private fun createDots(container: FrameLayout, dots: com.motionbreeze.data.DotSettings): List<DotView> {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val dotViews = mutableListOf<DotView>()

        for (i in 0 until dots.dotsPerSide) {
            val t = if (dots.dotsPerSide == 1) 0.5f else i.toFloat() / (dots.dotsPerSide - 1)
            val yOffset = (screenHeight * 0.1f + screenHeight * 0.8f * t).toInt()
            val depthFactor = 0.5f + 0.5f * Math.abs(t - 0.5f) * 2f

            val leftDot = DotView(this, depthFactor).apply {
                setDotSize(dots.dotSizeDp)
                setOpacityRange(dots.minOpacity, dots.maxOpacity)
            }
            val leftParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.START or Gravity.TOP
                leftMargin = 16
                topMargin = yOffset
            }
            container.addView(leftDot, leftParams)
            dotViews.add(leftDot)

            val rightDot = DotView(this, depthFactor).apply {
                setDotSize(dots.dotSizeDp)
                setOpacityRange(dots.minOpacity, dots.maxOpacity)
            }
            val rightParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                rightMargin = 16
                topMargin = yOffset
            }
            container.addView(rightDot, rightParams)
            dotViews.add(rightDot)
        }

        return dotViews
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
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            rebuildOverlayIfNeeded()
        }
        settingsRepository.registerListener(prefsListener)
    }

    private fun unregisterSettingsListener() {
        settingsRepository.unregisterListener(prefsListener)
    }

    private fun rebuildOverlayIfNeeded() {
        if (isOverlayMode && overlayView != null) {
            removeOverlayView()
            createOverlayView()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val settings = settingsRepository.readSettings().dots
        val alpha = 1f - (settings.smoothingMs.toFloat() / 1000f)

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
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_stop), stopPendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_stop), stopPendingIntent)
                .setOngoing(true)
                .build()
        }
    }

    companion object {
        const val CHANNEL_ID = "motion_breeze_overlay"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_OVERLAY = "com.motionbreeze.action.START_OVERLAY"
        const val ACTION_START_IN_APP = "com.motionbreeze.action.START_IN_APP"
        const val ACTION_STOP = "com.motionbreeze.action.STOP"

        var isRunning = false
            private set
    }
}