package com.motionbreeze.receiver

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.motionbreeze.service.OverlayService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ActivityTransitionReceiverTest {

    private lateinit var receiver: ActivityTransitionReceiver
    private lateinit var context: android.app.Application
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        receiver = ActivityTransitionReceiver()
        context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences("motion_breeze_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        OverlayService._runningState.value = false
    }

    @After
    fun tearDown() {
        OverlayService._runningState.value = false
        prefs.edit().clear().commit()
    }

    @Test
    fun `vehicle enter starts overlay when auto-activate is enabled`() {
        prefs.edit().putBoolean("auto_activate", true).commit()

        receiver.onVehicleEntered(context)

        val startedIntents = Shadows.shadowOf(context).nextStartedService
        assertNotNull("Expected startForegroundService to be called", startedIntents)
        assertEquals(
            OverlayService.ACTION_START_OVERLAY,
            startedIntents.action
        )
    }

    @Test
    fun `vehicle enter does not start when auto-activate is disabled`() {
        prefs.edit().putBoolean("auto_activate", false).commit()

        receiver.onVehicleEntered(context)

        val startedIntents = Shadows.shadowOf(context).nextStartedService
        assertEquals(
            null,
            startedIntents,
            "No service should be started when auto-activate is disabled"
        )
    }

    @Test
    fun `vehicle enter does not start when already running`() {
        prefs.edit().putBoolean("auto_activate", true).commit()
        OverlayService._runningState.value = true

        receiver.onVehicleEntered(context)

        val startedIntents = Shadows.shadowOf(context).nextStartedService
        assertEquals(
            null,
            startedIntents,
            "No duplicate start should occur when overlay is already running"
        )
    }

    @Test
    fun `vehicle exit triggers grace period alarm when auto-stop lock is off`() {
        OverlayService._runningState.value = true
        prefs.edit()
            .putBoolean("auto_activate", true)
            .putBoolean("auto_stop_lock", false)
            .commit()

        receiver.onVehicleExited(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val scheduledAlarm = shadowAlarm.nextScheduledAlarm

        assertNotNull("Expected a grace period alarm to be scheduled", scheduledAlarm)
        val pendingIntent = scheduledAlarm.operation
        assertNotNull("Expected a PendingIntent for the grace period stop", pendingIntent)
    }

    @Test
    fun `grace period cancelled on re-enter`() {
        OverlayService._runningState.value = true
        prefs.edit()
            .putBoolean("auto_activate", true)
            .putBoolean("auto_stop_lock", false)
            .commit()

        receiver.onVehicleExited(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val firstAlarm = shadowAlarm.nextScheduledAlarm
        assertNotNull("Expected a grace period alarm to be scheduled on exit", firstAlarm)

        receiver.onVehicleEntered(context)

        val cancelledAlarms = shadowAlarm.scheduledAlarms.filter {
            it.operation == firstAlarm.operation
        }
        assertTrue(
            "Grace period alarm should have been cancelled on re-enter",
            cancelledAlarms.isEmpty() || shadowAlarm.peekNextScheduledAlarm()?.operation != firstAlarm.operation
        )
    }

    @Test
    fun `grace period expired stops overlay`() {
        OverlayService._runningState.value = true

        val stopIntent = Intent(context, ActivityTransitionReceiver::class.java).apply {
            action = "com.motionbreeze.action.GRACE_PERIOD_STOP"
        }
        receiver.onReceive(context, stopIntent)

        val stopIntents = Shadows.shadowOf(context).stopServiceIntent
        assertNotNull("Expected the overlay to be stopped after grace period", stopIntents)
        assertEquals(
            OverlayService.ACTION_STOP,
            stopIntents.action
        )
    }

    @Test
    fun `auto-stop lock prevents vehicle exit from stopping overlay`() {
        OverlayService._runningState.value = true
        prefs.edit()
            .putBoolean("auto_activate", true)
            .putBoolean("auto_stop_lock", true)
            .commit()

        receiver.onVehicleExited(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarm = Shadows.shadowOf(alarmManager)
        val scheduledAlarm = shadowAlarm.nextScheduledAlarm

        assertEquals(
            null,
            scheduledAlarm,
            "No grace period alarm should be scheduled when auto-stop lock is enabled"
        )
    }

    @Test
    fun `confirm-before-start sends notification instead of starting overlay`() {
        prefs.edit()
            .putBoolean("auto_activate", true)
            .putBoolean("confirm_before_start", true)
            .commit()

        receiver.onVehicleEntered(context)

        val startedIntents = Shadows.shadowOf(context).nextStartedService
        assertEquals(
            null,
            startedIntents,
            "No service should be started when confirm-before-start is enabled"
        )

        val shadowNotificationManager = Shadows.shadowOf(
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        )
        val notifications = shadowNotificationManager.allNotifications
        assertTrue(
            "A confirmation notification should be shown",
            notifications.isNotEmpty()
        )
    }

    @Test
    fun `manual stop does not disable auto-activate`() {
        prefs.edit().putBoolean("auto_activate", true).commit()

        val stopIntent = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        context.startService(stopIntent)
        OverlayService._runningState.value = false

        val autoActivate = prefs.getBoolean("auto_activate", false)
        assertTrue(
            "Auto-activate should remain enabled after manual stop",
            autoActivate
        )
    }
}