// file: com/example/workation/util/Constants.kt
package com.example.workation.util

object Constants {
    const val ACTION_SERVICE_START        = "ACTION_SERVICE_START"
    const val ACTION_SERVICE_STOP         = "ACTION_SERVICE_STOP"
    const val ACTION_SERVICE_CANCEL       = "ACTION_SERVICE_CANCEL"

    const val NOTIFICATION_CHANNEL_ID     = "STOPWATCH_CHANNEL"
    const val NOTIFICATION_CHANNEL_NAME   = "Stopwatch Channel"
    const val NOTIFICATION_ID             = 1001

    // request codes pre PendingIntent
    const val CLICK_REQUEST_CODE   = 100
    const val CANCEL_REQUEST_CODE  = 101
    const val STOP_REQUEST_CODE    = 102
    const val RESUME_REQUEST_CODE  = 103
}
