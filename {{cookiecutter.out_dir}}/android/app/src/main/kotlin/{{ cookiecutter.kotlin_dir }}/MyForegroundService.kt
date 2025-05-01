package com.example.workation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MyForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "stopwatch_channel"
        private const val CHANNEL_NAME = "Stopwatch Service"
        private const val NOTIF_ID = 1
    }

    private val handler = Handler()
    private var elapsedSeconds = 0L

    private val updateRunnable = object : Runnable {
        override fun run() {
            elapsedSeconds++
            // Aktualizuj notifikáciu
            val notif = buildNotification(formatTime(elapsedSeconds))
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIF_ID, notif)
            // naplánuj ďalšiu aktualizáciu o 1 sekundu
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // spusti foreground s počiatočnou notifikáciou
        val notif = buildNotification(formatTime(elapsedSeconds))
        startForeground(NOTIF_ID, notif)
        // spusti timer
        handler.postDelayed(updateRunnable, 1000L)
        // ak systém zabije servis, neobnovovať ho
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // zastav handler pri zničení servisu
        handler.removeCallbacks(updateRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(timeText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stopwatch")
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_timer)  // pridaj si vlastnú ikonu v res/drawable
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for stopwatch foreground service"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(chan)
        }
    }

    private fun formatTime(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hrs, mins, secs)
    }
}






// package com.example.workation

// import android.app.Service
// import android.content.Intent
// import android.os.IBinder
// import android.os.Build
// import android.app.Notification
// import android.app.NotificationChannel
// import android.app.NotificationManager

// class MyForegroundService : Service() {

//     companion object {
//         const val CHANNEL_ID = "stopwatch_channel"
//         const val NOTIFICATION_ID = 1001
//     }

//     override fun onCreate() {
//         super.onCreate()
//         // Vytvoríme NotificationChannel pre Android 8+
//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//             val channel = NotificationChannel(
//                 CHANNEL_ID,
//                 "Stopwatch Channel",
//                 NotificationManager.IMPORTANCE_LOW
//             )
//             val mgr = getSystemService(NotificationManager::class.java)
//             mgr.createNotificationChannel(channel)
//         }
//     }

//     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//         // Postavíme základnú notifikáciu
//         val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//             Notification.Builder(this, CHANNEL_ID)
//                 .setContentTitle("Stopky")
//                 .setContentText("Timer beží…")
//                 .setSmallIcon(R.mipmap.ic_launcher)  // použime launcher ikonu
//                 .build()
//         } else {
//             @Suppress("DEPRECATION")
//             Notification.Builder(this)
//                 .setContentTitle("Stopky")
//                 .setContentText("Timer beží…")
//                 .setSmallIcon(R.mipmap.ic_launcher)
//                 .build()
//         }

//         // Tu prepneme servicu do foreground – Android ju nebude zabíjať
//         startForeground(NOTIFICATION_ID, notification)

//         // Keď python behá na pozadí (tvoj timer-thread), môže cez NotificationManager
//         // a Notification.Builder buď priamo alebo z tvojho Python kódu
//         // notifikáciu aktualizovať – práve to už máš hotové v update_notification().

//         return START_STICKY
//     }

//     override fun onBind(intent: Intent?): IBinder? = null
// }
