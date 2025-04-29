package com.example.workation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.stopwatch.MainActivity
import com.example.stopwatch.util.Constants.*

@ExperimentalAnimationApi
object ServiceHelper {

    private val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        PendingIntent.FLAG_IMMUTABLE
    else 0

    fun startPendingIntent(context: Context) = Intent(context, StopwatchService::class.java).apply {
        action = ACTION_SERVICE_START
    }.let { intent ->
        PendingIntent.getService(context, 0, intent, flags)
    }

    fun stopPendingIntent(context: Context) = Intent(context, StopwatchService::class.java).apply {
        action = ACTION_SERVICE_STOP
    }.let { intent ->
        PendingIntent.getService(context, 1, intent, flags)
    }

    fun cancelPendingIntent(context: Context) = Intent(context, StopwatchService::class.java).apply {
        action = ACTION_SERVICE_CANCEL
    }.let { intent ->
        PendingIntent.getService(context, 2, intent, flags)
    }

    fun clickPendingIntent(context: Context) = Intent(context, MainActivity::class.java).let { intent ->
        PendingIntent.getActivity(context, 3, intent, flags)
    }
}
