package com.motionbreeze.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.View

class DotView(
    context: Context,
    private val depthFactor: Float,
) : View(context) {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }

    private var dotSizePx = 8f * resources.displayMetrics.density
    private var minOpacity = 0.2f
    private var maxOpacity = 0.6f

    private var offsetX = 0f
    private var offsetY = 0f

    fun setDotSize(sizeDp: Int) {
        dotSizePx = sizeDp * resources.displayMetrics.density
        invalidate()
    }

    fun setOpacityRange(min: Float, max: Float) {
        minOpacity = min
        maxOpacity = max
    }

    fun updateMotion(lateralOffset: Float, longitudinalOffset: Float, intensity: Float) {
        val maxPixelOffset = 60f * resources.displayMetrics.density
        offsetX = (lateralOffset * depthFactor).coerceIn(-1f, 1f) * maxPixelOffset
        offsetY = (longitudinalOffset * depthFactor).coerceIn(-1f, 1f) * maxPixelOffset * 0.5f

        val alpha = (minOpacity + (maxOpacity - minOpacity) * intensity) * 255
        val clampedAlpha = alpha.coerceIn(0f, 255f).toInt()
        dotPaint.alpha = clampedAlpha
        outlinePaint.alpha = (clampedAlpha * 0.3f).coerceIn(0f, 255f).toInt()

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f + offsetX
        val cy = height / 2f + offsetY
        val radius = dotSizePx / 2f
        canvas.drawCircle(cx, cy, radius, dotPaint)
        canvas.drawCircle(cx, cy, radius, outlinePaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (dotSizePx * 4).toInt()
        setMeasuredDimension(size, size)
    }
}