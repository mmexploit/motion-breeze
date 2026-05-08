package com.motionbreeze.ui.screens.inapp

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.motionbreeze.data.SettingsRepository
import com.motionbreeze.service.OverlayService
import com.motionbreeze.ui.components.DotView
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

class MotionCueActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var settingsRepository: SettingsRepository
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null

    private var smoothedAccelX = 0f
    private var smoothedAccelY = 0f
    private var smoothedGyroX = 0f
    private var smoothedGyroY = 0f

    private var dotViews = emptyList<DotView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        settingsRepository = (application as com.motionbreeze.MotionBreezeApp).settingsRepository
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD000000.toInt())
            setPadding(32, 64, 32, 32)
        }

        val dotsContainer = FrameLayout(this).apply {
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300,
            ).apply {
                setMargins(0, 32, 0, 32)
            }
            layoutParams = params
            setBackgroundColor(0x33000000.toInt())
        }

        val infoText = TextView(this).apply {
            text = "Motion cues are active within this screen.\nTilt or move your phone to see the dots respond."
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }

        val enableButton = android.widget.Button(this).apply {
            text = "Enable Full Overlay →"
            setBackgroundColor(0xFF0066A6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${this@MotionCueActivity.packageName}")
                )
                startActivity(intent)
            }
        }

        val closeButton = android.widget.Button(this).apply {
            text = "Close"
            setTextColor(0xCCFFFFFF.toInt())
            setOnClickListener {
                finish()
            }
        }

        rootLayout.addView(dotsContainer)
        rootLayout.addView(infoText)
        rootLayout.addView(enableButton)
        rootLayout.addView(closeButton)
        setContentView(rootLayout)

        val settings = settingsRepository.readSettings().dots
        dotViews = createDemoDots(dotsContainer, settings)
    }

    private fun createDemoDots(container: FrameLayout, dots: com.motionbreeze.data.DotSettings): List<DotView> {
        val displayMetrics = resources.displayMetrics
        val containerWidth = 800
        val containerHeight = 300
        val density = displayMetrics.density

        val edgePadding = (containerWidth * 0.02f).coerceAtLeast(12f * density).toInt()

        val dotViewsList = mutableListOf<DotView>()

        for (i in 0 until dots.dotsPerSide) {
            val t = if (dots.dotsPerSide == 1) 0.5f else i.toFloat() / (dots.dotsPerSide - 1)
            val yOffset = (containerHeight * 0.1f + containerHeight * 0.8f * t).toInt()
            val depthFactor = 0.5f + 0.5f * abs(t - 0.5f) * 2f

            val leftDot = DotView(this, depthFactor).apply {
                setDotSize(dots.dotSizeDp)
                setOpacityRange(dots.minOpacity, dots.maxOpacity)
            }
            val leftParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
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
                leftMargin = containerWidth - rightDot.layoutParams.width - edgePadding
                topMargin = yOffset
            }
            container.addView(rightDot, rightParams)
            dotViewsList.add(rightDot)
        }

        return dotViewsList
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            switchToOverlayMode()
            return
        }
        registerSensorListener()
    }

    override fun onPause() {
        super.onPause()
        unregisterSensorListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterSensorListener()
    }

    private fun registerSensorListener() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val settings = settingsRepository.readSettings().dots
        val tau = settings.smoothingMs / 1000.0
        val dt = 1.0 / 60.0
        val alpha = (1.0 - exp(-dt / tau)).toFloat()

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

        val motionIntensity = sqrt(lateralMotion * lateralMotion + longitudinalMotion * longitudinalMotion)
        val intensityClamp = (motionIntensity / 5f).coerceIn(0f, 1f)

        for (dot in dotViews) {
            dot.updateMotion(lateralMotion, longitudinalMotion, intensityClamp)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}