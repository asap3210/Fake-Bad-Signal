package com.asap3210.fakebadsignal

import android.content.Context
import android.media.AudioManager
import kotlin.random.Random

/**
 * Efecto de "mala cobertura" durante una llamada:
 *  - Corta tu micrófono a ráfagas (el otro te oye entrecortado).
 *  - Opcionalmente añade ruido por el altavoz (solo se capta con altavoz activado).
 *
 * Nota: silenciar el micrófono del sistema durante la llamada depende del móvil y de la
 * versión de Android; en algunos modelos puede no tener efecto.
 */
class CallEffect(private val ctx: Context) {
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val noise = NoiseEngine()
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        if (Prefs.noise(ctx)) {
            noise.mode = NoiseMode.MIXED
            noise.intensity = Prefs.intensity(ctx)
            noise.start()
        }
        thread = Thread {
            try {
                while (running) {
                    val k = Prefs.intensity(ctx).coerceIn(0.05f, 1f)
                    // Tramo con voz normal: más corto cuanto más intensidad
                    sleep(talkMs(k))
                    if (!running) break
                    audio.isMicrophoneMute = true
                    // Corte: más largo cuanto más intensidad
                    sleep(cutMs(k))
                    audio.isMicrophoneMute = false
                }
            } finally {
                audio.isMicrophoneMute = false
            }
        }.also { it.isDaemon = true; it.start() }
    }

    companion object {
        /** Duración (ms) del tramo con voz normal: más corto cuanto más intensidad. */
        fun talkMs(k: Float): Long = (250 + Random.nextDouble() * 2500 * (1.1 - k)).toLong()
        /** Duración (ms) de cada corte: más largo cuanto más intensidad. */
        fun cutMs(k: Float): Long = (80 + Random.nextDouble() * 700 * k).toLong()
    }

    private fun sleep(ms: Long) = try { Thread.sleep(ms) } catch (_: InterruptedException) {}

    fun stop() {
        running = false
        thread?.interrupt()
        thread?.join(500)
        thread = null
        noise.stop()
        audio.isMicrophoneMute = false
    }
}
