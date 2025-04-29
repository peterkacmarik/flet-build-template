// file: com/example/workation/service/StopwatchService.kt
package com.example.workation.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.workation.MainActivity                  // ← import MainActivity
import com.example.workation.R                           // ← import R pre drawable
import com.example.workation.util.Constants
import com.example.workation.util.formatTime
import com.example.workation.util.pad
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class StopwatchService : Service() {

    private val binder = StopwatchBinder()
    private var duration: Duration = Duration.ZERO
    private lateinit var timer: Timer

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var hours = "00"
    private var minutes = "00"
    private var seconds = "00"

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        initNotificationBuilder()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_SERVICE_START  -> startStopwatch()
            Constants.ACTION_SERVICE_STOP   -> pauseStopwatch()
            Constants.ACTION_SERVICE_CANCEL -> cancelStopwatch()
        }
        return START_STICKY
    }

    private fun initNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationBuilder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Stopwatch")
            .setContentText("00:00:00")
            // tu môžeš namiesto android.R.drawable.ic_media_play
            // použiť svoj vlastný R.drawable.ic_baseline_timer_24
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(createClickIntent())
    }

    private fun createClickIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            Constants.CLICK_REQUEST_CODE,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE else 0
        )
    }

    private fun createActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, StopwatchService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE else 0
        )
    }

    private fun startStopwatch() {
        notificationBuilder.clearActions()
            .addAction(0, "Pause",  createActionIntent(Constants.ACTION_SERVICE_STOP,  Constants.STOP_REQUEST_CODE))
            .addAction(0, "Cancel", createActionIntent(Constants.ACTION_SERVICE_CANCEL, Constants.CANCEL_REQUEST_CODE))
        startForeground(Constants.NOTIFICATION_ID, notificationBuilder.build())

        timer = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
            duration += 1.seconds
            duration.toComponents { h, m, s, _ ->
                hours   = h.toInt().pad()
                minutes = m.pad()
                seconds = s.pad()
            }
            notificationManager.notify(
                Constants.NOTIFICATION_ID,
                notificationBuilder.setContentText(formatTime(hours, minutes, seconds)).build()
            )
        }
    }

    private fun pauseStopwatch() {
        if (this::timer.isInitialized) timer.cancel()
        notificationBuilder.clearActions()
            .addAction(0, "Resume", createActionIntent(Constants.ACTION_SERVICE_START, Constants.RESUME_REQUEST_CODE))
            .addAction(0, "Cancel", createActionIntent(Constants.ACTION_SERVICE_CANCEL, Constants.CANCEL_REQUEST_CODE))
        notificationManager.notify(
            Constants.NOTIFICATION_ID,
            notificationBuilder.setContentText(formatTime(hours, minutes, seconds)).build()
        )
    }

    private fun cancelStopwatch() {
        if (this::timer.isInitialized) timer.cancel()
        duration = Duration.ZERO
        hours = "00"; minutes = "00"; seconds = "00"
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        if (this::timer.isInitialized) timer.cancel()
        super.onDestroy()
    }

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }
}
