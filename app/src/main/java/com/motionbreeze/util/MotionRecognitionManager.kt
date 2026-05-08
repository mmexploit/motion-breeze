package com.motionbreeze.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.motionbreeze.receiver.ActivityTransitionReceiver

object MotionRecognitionManager {

    private const val TAG = "MotionRecognition"
    private const val REQUEST_CODE = 1001

    fun requestActivityUpdates(context: Context) {
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
        )

        val request = ActivityTransitionRequest(transitions)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ActivityTransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val client = ActivityRecognition.getClient(context)
        client.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Activity transition updates requested") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to request activity transitions: ${e.message}") }
    }

    fun removeActivityUpdates(context: Context) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ActivityTransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val client = ActivityRecognition.getClient(context)
        client.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener { Log.d(TAG, "Activity transition updates removed") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to remove activity transitions: ${e.message}") }
    }
}