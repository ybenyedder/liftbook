package com.hevyclone.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.hevyclone.app.ui.RestTimer

/**
 * Foreground service driving the rest-timer notification.
 *
 * Standard Android notification template (like Hevy): title "Repos 1:23",
 * system progress bar tinted with the accent, −15s / +15s / Passer as system
 * action buttons. No custom RemoteViews — since Android 12 the system wraps
 * custom backgrounds in its own themed container, which rendered our old black
 * card as an ugly square.
 *
 * The state lives in [RestTimer] (single source of truth, observed by the in-app RestBar);
 * this service only renders it every 500 ms and handles the notification button actions.
 * When the countdown hits 0 a soft bell chime (res/raw/rest_done.wav) plays once.
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

        /** How long the "Repos terminé" state is shown after the timer hits 0. */
        private const val FINISHED_LINGER_MS = 4000L

        /** Accent blue (same as the in-app rest bar progress line). */
        private val ACCENT = 0xFF028CFD.toInt()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { tickNow() }

    /** Total duration of the current rest in seconds (progress-bar max). */
    private var totalSec = 0

    /** Timestamp at which the finished state was entered (0 while running). */
    private var finishedSince = 0L

    private var player: MediaPlayer? = null

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
        releasePlayer()
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
            if (finishedSince == 0L) {
                finishedSince = now
                playDoneChime()
            }
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
        releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun remainingSec(): Int {
        val remainMs = RestTimer.endAt - System.currentTimeMillis()
        if (remainMs <= 0L) return 0
        return ((remainMs + 999L) / 1000L).toInt() // ceil, so "90" shows for the full 90 s
    }

    /**
     * Soft bell chime when the rest countdown reaches 0 (Hevy-like).
     *
     * The chime plays on the notification stream when that stream is audible,
     * but falls back to the ALARM stream (audible even in silent/vibrate mode
     * and through DND) when the phone is silenced or notification volume is 0 —
     * the typical gym setup where the sound would otherwise never be heard.
     * A short double vibration accompanies it either way.
     */
    private fun playDoneChime() {
        runCatching {
            val vib = getSystemService(Vibrator::class.java)
            vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 90, 70), -1))
        }
        runCatching {
            releasePlayer()
            val am = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val usage = if (am.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL &&
                am.getStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION) > 0
            ) AudioAttributes.USAGE_NOTIFICATION else AudioAttributes.USAGE_ALARM
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            val afd: AssetFileDescriptor = resources.openRawResourceFd(R.raw.rest_done) ?: return
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { p, _, _ -> p.release(); true }
            mp.prepare()
            mp.start()
            player = mp
        }
    }

    private fun releasePlayer() {
        runCatching { player?.let { if (it.isPlaying) it.stop() } }
        runCatching { player?.release() }
        player = null
    }

    private fun buildNotification(): android.app.Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Minuteur de repos", NotificationManager.IMPORTANCE_LOW)
        )
        val content = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val finished = RestTimer.endAt > 0 && System.currentTimeMillis() >= RestTimer.endAt
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(ACCENT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(content)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (finished) {
            builder.setContentTitle(L10nText.restDone(this))
                .setProgress(totalSec.coerceAtLeast(1), 0, false)
        } else {
            val remain = remainingSec()
            builder.setContentTitle(L10nText.restRemaining(this, remain))
                .setProgress(totalSec.coerceAtLeast(1), remain, false)
        }
        builder.addAction(0, L10nText.actMinus15(this), actionPendingIntent(ACTION_MINUS15, 1))
        builder.addAction(0, L10nText.actPlus15(this), actionPendingIntent(ACTION_ADD15, 2))
        builder.addAction(0, L10nText.actSkip(this), actionPendingIntent(ACTION_SKIP, 3))
        return builder.build()
    }

    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
            this, requestCode,
            Intent(this, RestNotifService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun startForegroundCompat(notif: android.app.Notification) {
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

/** Tiny localizer for the notification texts (service-side, no Compose). */
private object L10nText {
    private fun fr(ctx: Context): Boolean =
        ctx.resources.configuration.locales.get(0)?.language == "fr"

    fun restRemaining(ctx: Context, sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        val t = "$m:${String.format("%02d", s)}"
        return if (fr(ctx)) "Repos $t" else "Rest $t"
    }

    fun restDone(ctx: Context): String =
        if (fr(ctx)) "Repos terminé" else "Rest complete"

    fun actMinus15(ctx: Context): String =
        if (fr(ctx)) "− 15 s" else "-15s"

    fun actPlus15(ctx: Context): String =
        if (fr(ctx)) "+ 15 s" else "+15s"

    fun actSkip(ctx: Context): String =
        if (fr(ctx)) "Passer" else "Skip"

    fun workoutTitle(ctx: Context): String =
        if (fr(ctx)) "Entraînement" else "Workout"
}
