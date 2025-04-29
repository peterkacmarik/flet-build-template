package com.example.workation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.example.workation.util.Constants.*
import com.example.workation.util.formatTime
import com.example.workation.util.pad
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@ExperimentalAnimationApi
@AndroidEntryPoint
class StopwatchService : Service() {
    @Inject lateinit var notificationManager: NotificationManager
    @Inject lateinit var notificationBuilder: NotificationCompat.Builder

    private val binder = StopwatchBinder()
    private var duration: Duration = Duration.ZERO
    private lateinit var timer: Timer

    var seconds = mutableStateOf("00")
        private set
    var minutes = mutableStateOf("00")
        private set
    var hours   = mutableStateOf("00")
        private set
    var currentState = mutableStateOf(StopwatchState.Idle)
        private set

    override fun onBind(intent: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START  -> startTimer()
            ACTION_SERVICE_STOP   -> pauseTimer()
            ACTION_SERVICE_CANCEL -> cancelTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        currentState.value = StopwatchState.Started
        setupForeground()
        timer = fixedRateTimer(period = 1000L) {
            duration += 1.seconds
            updateTimeUnits()
            updateNotification(
                hours = hours.value,
                minutes = minutes.value,
                seconds = seconds.value
            )
        }
    }

    private fun pauseTimer() {
        if (this::timer.isInitialized) timer.cancel()
        currentState.value = StopwatchState.Stopped
        updateNotification(
            hours = hours.value,
            minutes = minutes.value,
            seconds = seconds.value
        )
    }

    private fun cancelTimer() {
        pauseTimer()
        duration = Duration.ZERO
        currentState.value = StopwatchState.Idle
        updateTimeUnits()
        stopForegroundService()
    }

    private fun updateTimeUnits() {
        duration.toComponents { h, m, s, _ ->
            hours.value   = h.toInt().pad()
            minutes.value = m.pad()
            seconds.value = s.pad()
        }
    }

    private fun setupForeground() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notificationBuilder
            .setContentText(formatTime(hours.value, minutes.value, seconds.value))
            .addAction(0, "Stop", ServiceHelper.stopPendingIntent(this))
            .addAction(0, "Cancel", ServiceHelper.cancelPendingIntent(this))
            .build()
        )
    }

    private fun stopForegroundService() {
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (this::timer.isInitialized) timer.cancel()
        super.onDestroy()
    }

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }
}