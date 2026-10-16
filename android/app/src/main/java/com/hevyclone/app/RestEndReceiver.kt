package com.hevyclone.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

/**
 * Fires exactly at RestTimer.endAt via AlarmManager, even if the foreground service
 * was frozen or killed while the app sat in the background. Plays the end-of-rest
 * chime (stronger + longer) and posts a high-priority "Repos terminé" notification.
 *
 * The service also plays the chime when its own tick sees endAt pass — this receiver
 * is the safety net for the "notification timer froze while I wasn't in the app" case.
 */
class RestEndReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.hevyclone.app.rest.END"
        private const val REQ = 4243
        private const val CHANNEL = "rest_done"

        fun schedule(ctx: Context, endAtMs: Long) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val pi = PendingIntent.getBroadcast(
                ctx, REQ,
                Intent(ctx, RestEndReceiver::class.java).setAction(ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            // setAlarmClock is exact + exempt from Doze, no special permission needed.
            val info = android.app.AlarmManager.AlarmClockInfo(endAtMs, pi)
            am.setAlarmClock(info, pi)
        }

        fun cancel(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.cancel(
                PendingIntent.getBroadcast(
                    ctx, REQ,
                    Intent(ctx, RestEndReceiver::class.java).setAction(ACTION),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        }
    }

    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        // Timer was cleared/skipped — stay silent.
        if (com.hevyclone.app.ui.RestTimer.endAt <= 0L) return
        playChime(ctx)
        postDoneNotif(ctx)
    }

    private fun playChime(ctx: Context) {
        runCatching {
            val vib = ctx.getSystemService(Vibrator::class.java)
            vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 90, 120, 90, 120, 250), -1))
        }
        runCatching {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
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
            val afd: AssetFileDescriptor = ctx.resources.openRawResourceFd(R.raw.rest_done) ?: return
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setVolume(1.0f, 1.0f)
            mp.prepare()
            mp.setOnCompletionListener { p ->
                Handler(Looper.getMainLooper()).postDelayed({
                    runCatching { p.seekTo(0); p.start() }.onFailure { runCatching { p.release() } }
                }, 350)
            }
            mp.setOnErrorListener { pl, _, _ -> pl.release(); true }
            mp.start()
        }
    }

    private fun postDoneNotif(ctx: Context) {
        runCatching {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Fin de repos", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 90, 120, 90, 120, 250)
                    setSound(null, null)
                }
            )
            val fr = ctx.resources.configuration.locales.get(0)?.language == "fr"
            val notif = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(0xFF028CFD.toInt())
                .setContentTitle(if (fr) "✅ Repos terminé — à toi !" else "✅ Rest complete — go!")
                .setContentText(if (fr) "Prêt pour la prochaine série" else "Ready for the next set")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 90, 120, 90, 120, 250))
                .setContentIntent(
                    PendingIntent.getActivity(
                        ctx, 0,
                        Intent(ctx, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                )
                .build()
            nm.notify(4244, notif)
        }
    }
}
