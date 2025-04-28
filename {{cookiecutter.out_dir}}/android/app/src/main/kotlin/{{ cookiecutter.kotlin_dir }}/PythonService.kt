package com.example.workation

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class PythonService : Service() {

    companion object {
        const val CHANNEL_ID      = "python_service_channel"
        const val NOTIFICATION_ID = 42
    }

    override fun onCreate() {
        super.onCreate()
        // 1) Naštartovať Python runtime
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        // 2) Vytvoriť notification channel
        val mgr = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Python Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(chan)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 3) Zavoláme service/main.py:main()
        Python.getInstance()
              .getModule("service.main")
              .callAttr("main")

        // 4) Postavíme základnú notifikáciu
        val noti = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("WorkAtion Service")
                .setContentText("Timer beží na pozadí…")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("WorkAtion Service")
                .setContentText("Timer beží na pozadí…")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        }
        startForeground(NOTIFICATION_ID, noti)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
