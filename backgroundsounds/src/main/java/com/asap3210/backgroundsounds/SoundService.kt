package com.asap3210.backgroundsounds

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/** Reproduce el sonido en segundo plano, con la pantalla apagada y con temporizador opcional. */
class SoundService : Service() {
    companion object {
        const val ACTION_PLAY = "play"
        const val ACTION_VOLUME = "volume"
        const val ACTION_STOP = "stop"
        const val EXTRA_KIND = "kind"
        const val EXTRA_VOLUME = "vol"
        const val EXTRA_MINUTES = "min"
    }

    private lateinit var engine: AmbientEngine
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopAll() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        engine = AmbientEngine(this, AudioAttributes.USAGE_MEDIA)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val kind = runCatching { Ambient.valueOf(intent.getStringExtra(EXTRA_KIND)!!) }
                    .getOrNull() ?: return START_NOT_STICKY
                val vol = intent.getFloatExtra(EXTRA_VOLUME, 0.6f)
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
                ServiceCompat.startForeground(
                    this, 1, notification(kind), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
                engine.play(kind, vol)
                PlayerState.playing = kind
                handler.removeCallbacks(stopRunnable)
                if (minutes > 0) handler.postDelayed(stopRunnable, minutes * 60_000L)
            }
            ACTION_VOLUME -> engine.setVolume(intent.getFloatExtra(EXTRA_VOLUME, 0.6f))
            ACTION_STOP -> stopAll()
        }
        return START_NOT_STICKY
    }

    private fun notification(kind: Ambient): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("sounds", "Background Sounds", NotificationManager.IMPORTANCE_LOW)
        )
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, SoundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "sounds")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("${kind.emoji} ${kind.label}")
            .setContentText("Sonido de fondo en reproducción")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stop)
            .build()
    }

    private fun stopAll() {
        handler.removeCallbacks(stopRunnable)
        engine.stop()
        PlayerState.playing = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopRunnable)
        engine.stop()
        PlayerState.playing = null
        super.onDestroy()
    }
}
