package com.motionbreeze.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.motionbreeze.service.OverlayService

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent) ?: return

            for (event in result.transitionEvents) {
                Log.d(TAG, "Transition: type=${activityTypeToString(event.activityType)} " +
                        "transition=${transitionTypeToString(event.transitionType)}")

                if (event.activityType == DetectedActivity.IN_VEHICLE) {
                    when (event.transitionType) {
                        0 -> onVehicleEntered(context)
                        1 -> onVehicleExited(context)
                    }
                }
            }
        }
    }

    private fun onVehicleEntered(context: Context) {
        val settings = (context.applicationContext as com.motionbreeze.MotionBreezeApp)
            .settingsRepository.readSettings()
        if (!settings.autoActivate.autoActivate) {
            Log.d(TAG, "Auto-activate disabled — skipping")
            return
        }

        if (OverlayService.isRunning) {
            Log.d(TAG, "Overlay already running — skipping")
            return
        }

        Log.d(TAG, "Vehicle detected — starting overlay")

        if (settings.autoActivate.confirmBeforeStart) {
            showConfirmationNotification(context)
        } else {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START_OVERLAY
            }
            context.startForegroundService(intent)
        }
    }

    private fun onVehicleExited(context: Context) {
        if (!OverlayService.isRunning) return
        Log.d(TAG, "Vehicle motion ended — stopping overlay")
        val intent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun showConfirmationNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager

        val channel = android.app.NotificationChannel(
            CONFIRM_CHANNEL_ID,
            "Vehicle Detected",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Confirm motion cues activation"
        }
        notificationManager.createNotificationChannel(channel)

        val startIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_OVERLAY
        }
        val startPendingIntent = android.app.PendingIntent.getService(
            context, 0, startIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CONFIRM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("Vehicle detected")
                .setContentText("Tap to enable motion cues")
                .setContentIntent(startPendingIntent)
                .setAutoCancel(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle("Vehicle detected")
                .setContentText("Tap to enable motion cues")
                .setContentIntent(startPendingIntent)
                .setAutoCancel(true)
                .build()
        }

        notificationManager.notify(CONFIRM_NOTIFICATION_ID, notification)
    }

    private fun activityTypeToString(type: Int): String = when (type) {
        DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
        DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
        DetectedActivity.ON_FOOT -> "ON_FOOT"
        DetectedActivity.RUNNING -> "RUNNING"
        DetectedActivity.STILL -> "STILL"
        DetectedActivity.WALKING -> "WALKING"
        else -> "UNKNOWN($type)"
    }

    private fun transitionTypeToString(type: Int): String = when (type) {
        0 -> "ENTER"
        1 -> "EXIT"
        else -> "UNKNOWN($type)"
    }

    companion object {
        private const val TAG = "ActivityTransition"
        private const val CONFIRM_CHANNEL_ID = "motion_breeze_confirm"
        private const val CONFIRM_NOTIFICATION_ID = 2
    }
}