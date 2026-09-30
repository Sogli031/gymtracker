package com.gymtracker.app.util

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Foreground servis koji drži obaveštenje sa štopericom pauze. */
class RestTimerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startMs = intent?.getLongExtra(EXTRA_START_MS, 0L) ?: 0L
        if (startMs == 0L) {
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = RestNotifier.build(this, startMs)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                RestNotifier.NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(RestNotifier.NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    companion object {
        const val EXTRA_START_MS = "start_ms"
    }
}
