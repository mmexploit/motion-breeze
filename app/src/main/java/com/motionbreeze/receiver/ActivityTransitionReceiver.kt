package com.motionbreeze.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
                        TRANSITION_ENTER -> onVehicleEntered(context)
                        TRANSITION_EXIT -> onVehicleExited(context)
                    }
                }
            }
        }

        if (ACTION_GRACE_PERIOD_STOP == intent.action) {
            onGracePeriodExpired(context)
        }
    }

    internal fun onVehicleEntered(context: Context) {
        cancelPendingStop(context)

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
            val overlayIntent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START_OVERLAY
            }
            context.startForegroundService(overlayIntent)
        }
    }

    private fun onVehicleExited(context: Context) {
        if (!OverlayService.isRunning) return

        val settings = (context.applicationContext as com.motionbreeze.MotionBreezeApp)
            .settingsRepository.readSettings()

        if (settings.autoActivate.autoStopLock) {
            Log.d(TAG, "Auto-stop lock enabled — ignoring vehicle exit")
            return
        }

        Log.d(TAG, "Vehicle exit detected — starting 2-minute grace period")
        scheduleGracePeriodStop(context)
    }

    private fun scheduleGracePeriodStop(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val stopIntent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ACTION_GRACE_PERIOD_STOP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GRACE_PERIOD_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerTime = SystemClock.elapsedRealtime() + GRACE_PERIOD_MS
        try {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot set exact alarm, using inexact: ${e.message}")
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun cancelPendingStop(context: Context) {
        val stopIntent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = ACTION_GRACE_PERIOD_STOP
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GRACE_PERIOD_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Grace period timer cancelled (vehicle re-enter)")
    }

    private fun onGracePeriodExpired(context: Context) {
        if (!OverlayService.isRunning) return
        Log.d(TAG, "Grace period expired — stopping overlay")
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
        val startPendingIntent = PendingIntent.getService(
            context, 0, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        TRANSITION_ENTER -> "ENTER"
        TRANSITION_EXIT -> "EXIT"
        else -> "UNKNOWN($type)"
    }

    companion object {
        private const val TAG = "ActivityTransition"
        private const val CONFIRM_CHANNEL_ID = "motion_breeze_confirm"
        private const val CONFIRM_NOTIFICATION_ID = 2
        private const val TRANSITION_ENTER = 0
        private const val TRANSITION_EXIT = 1
        private const val GRACE_PERIOD_REQUEST_CODE = 2001
        const val GRACE_PERIOD_MS = 2 * 60 * 1000L
        private const val ACTION_GRACE_PERIOD_STOP = "com.motionbreeze.action.GRACE_PERIOD_STOP"
    }
}