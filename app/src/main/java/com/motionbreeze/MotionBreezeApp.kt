package com.motionbreeze

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.motionbreeze.service.OverlayService

class MotionBreezeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            OverlayService.CHANNEL_ID,
            getString(R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.overlay_notification_channel)
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}