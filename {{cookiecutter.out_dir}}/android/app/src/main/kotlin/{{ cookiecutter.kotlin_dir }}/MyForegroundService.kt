package com.example.workation

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

class MyForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "stopwatch_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        // Vytvoríme NotificationChannel pre Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stopwatch Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Postavíme základnú notifikáciu
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Stopky")
                .setContentText("Timer beží…")
                .setSmallIcon(R.mipmap.ic_launcher)  // použime launcher ikonu
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Stopky")
                .setContentText("Timer beží…")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        }

        // Tu prepneme servicu do foreground – Android ju nebude zabíjať
        startForeground(NOTIFICATION_ID, notification)

        // Keď python behá na pozadí (tvoj timer-thread), môže cez NotificationManager
        // a Notification.Builder buď priamo alebo z tvojho Python kódu
        // notifikáciu aktualizovať – práve to už máš hotové v update_notification().

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
