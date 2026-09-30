package com.gymtracker.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gymtracker.app.MainActivity
import com.gymtracker.app.R

/** Informativno obaveštenje sa štopericom (vreme od poslednje unete serije), drži ga foreground servis. */
object RestNotifier {
    private const val CHANNEL_ID = "rest_timer_v2"
    const val NOTIFICATION_ID = 4711

    fun show(context: Context, startMs: Long) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, RestTimerService::class.java)
                .putExtra(RestTimerService.EXTRA_START_MS, startMs)
        )
    }

    fun build(context: Context, startMs: Long): Notification {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Štoperica pauze", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        setShowBadge(false)
                        setSound(null, null)
                        enableVibration(false)
                    }
            )
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val base = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMs)
        fun timerView(layout: Int) = RemoteViews(context.packageName, layout).apply {
            setChronometer(R.id.rest_chronometer, base, null, true)
        }
        val view = timerView(R.layout.notification_rest_timer)
        val smallView = timerView(R.layout.notification_rest_timer_small)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(smallView)
            .setCustomBigContentView(view)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .build()
    }

    fun cancel(context: Context) {
        context.stopService(Intent(context, RestTimerService::class.java))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
