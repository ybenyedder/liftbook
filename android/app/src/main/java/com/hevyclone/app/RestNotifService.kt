package com.hevyclone.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.hevyclone.app.ui.RestTimer

/**
 * Foreground service driving the Hevy-style rest-timer notification:
 * true-black custom layout, live countdown + progress bar, and −15s / +15s / Passer
 * buttons actionable straight from the notification.
 *
 * The state lives in [RestTimer] (single source of truth, observed by the in-app RestBar);
 * this service only renders it every 500 ms and handles the notification button actions.
 */
class RestNotifService : Service() {

    companion object {
        const val ACTION_START = "com.hevyclone.app.rest.START"
        const val ACTION_ADD15 = "com.hevyclone.app.rest.ADD15"
        const val ACTION_MINUS15 = "com.hevyclone.app.rest.MINUS15"
        const val ACTION_SKIP = "com.hevyclone.app.rest.SKIP"
        const val EXTRA_TOTAL = "total"

        /** Same notification id the old RestTimer notification used. */
        const val NOTIF_ID = 4242
        private const val CHANNEL = "rest_timer"
        private const val TICK_MS = 500L

        /** How long the "REPOS TERMINÉ" state is shown after the timer hits 0. */
        private const val FINISHED_LINGER_MS = 4000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { tickNow() }

    /** Total duration of the current rest in seconds (progress-bar max). */
    private var totalSec = 0

    /** Timestamp at which the finished state was entered (0 while running). */
    private var finishedSince = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD15 -> RestTimer.plus15()
            ACTION_MINUS15 -> RestTimer.minus15()
            ACTION_SKIP -> RestTimer.skip()
            else -> {
                totalSec = intent?.getIntExtra(EXTRA_TOTAL, 0) ?: 0
                if (totalSec <= 0) totalSec = remainingSec().coerceAtLeast(1)
            }
        }
        // Every start issued through PendingIntent.getForegroundService requires a
        // startForeground() call — re-asserting it here also refreshes the notif.
        startForegroundCompat(buildNotification())
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_NOT_STICKY // post-mortem recovery goes through RestTimer.restore()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun tickNow() {
        val now = System.currentTimeMillis()
        val endAt = RestTimer.endAt
        if (endAt <= 0L) {
            // Timer was cleared from the app — stop silently, never resurrect the notif.
            stopEverything()
            return
        }
        if (now < endAt) {
            finishedSince = 0L
            renotify()
            handler.postDelayed(tick, TICK_MS)
        } else {
            if (finishedSince == 0L) finishedSince = now
            renotify()
            if (now - finishedSince >= FINISHED_LINGER_MS) {
                RestTimer.onFinishedByService()
                stopEverything()
            } else {
                handler.postDelayed(tick, TICK_MS)
            }
        }
    }

    private fun stopEverything() {
        handler.removeCallbacks(tick)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun remainingSec(): Int {
        val remainMs = RestTimer.endAt - System.currentTimeMillis()
        if (remainMs <= 0L) return 0
        return ((remainMs + 999L) / 1000L).toInt() // ceil, so "90" shows for the full 90 s
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Minuteur de repos", NotificationManager.IMPORTANCE_LOW)
        )
        val finished = RestTimer.endAt > 0 && System.currentTimeMillis() >= RestTimer.endAt
        val rv = RemoteViews(packageName, R.layout.notif_rest)
        if (finished) {
            rv.setTextViewText(R.id.rest_label, "REPOS TERMINÉ")
            rv.setTextViewText(R.id.rest_time, "0:00")
            rv.setProgressBar(R.id.rest_progress, totalSec.coerceAtLeast(1), 0, false)
        } else {
            val remain = remainingSec()
            rv.setTextViewText(R.id.rest_label, "REPOS")
            rv.setTextViewText(R.id.rest_time, "${remain / 60}:${String.format("%02d", remain % 60)}")
            rv.setProgressBar(R.id.rest_progress, totalSec.coerceAtLeast(1), remain, false)
        }
        rv.setOnClickPendingIntent(R.id.btn_minus15, actionPendingIntent(ACTION_MINUS15, 1))
        rv.setOnClickPendingIntent(R.id.btn_plus15, actionPendingIntent(ACTION_ADD15, 2))
        rv.setOnClickPendingIntent(R.id.btn_skip, actionPendingIntent(ACTION_SKIP, 3))
        val content = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(android.graphics.Color.BLACK)
            .setContentIntent(content)
            .setCustomContentView(rv)
            .setCustomBigContentView(rv)
            .build()
    }

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
            this, requestCode,
            Intent(this, RestNotifService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun startForegroundCompat(notif: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            ServiceCompat.startForeground(this, NOTIF_ID, notif, 0)
        }
    }

    private fun renotify() {
        runCatching {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIF_ID, buildNotification())
        }
    }
}
