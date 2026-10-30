package com.asap3210.fakebadsignal

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.max
import kotlin.random.Random

enum class NoiseMode(val label: String) {
    STATIC("Estática"),
    CRACKLE("Crujidos"),
    GLITCH("Entrecortado"),
    MIXED("Mixto (auto)")
}

/**
 * Genera en tiempo real ruido de "mala señal" y lo reproduce por el canal de voz.
 * Con la llamada en altavoz, el micrófono lo capta y el otro lado lo oye como interferencia.
 */
class NoiseEngine {
    private val sampleRate = 16000
    @Volatile var mode: NoiseMode = NoiseMode.MIXED
    @Volatile var intensity: Float = 0.6f          // 0..1
    @Volatile private var forcedBurstSamples = 0

    @Volatile private var running = false
    private var thread: Thread? = null

    // Estado del generador
    private var lp = 0f
    private var held = 0f
    private var holdCount = 0
    private var segmentLeft = 0
    private var inBurst = false
    private var cur = NoiseMode.STATIC

    val isRunning get() = running

    /** Fuerza un corte de señal inmediato de ~1.5 s. */
    fun burstNow() { forcedBurstSamples = sampleRate * 3 / 2 }

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            val buf = ShortArray(1024)
            while (running) {
                fill(buf)
                track.write(buf, 0, buf.size)
            }
            track.stop()
            track.release()
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun nextSegment() {
        val k = intensity.coerceIn(0.05f, 1f)
        if (inBurst) {
            // Pausa: más corta cuanto más intensidad
            inBurst = false
            segmentLeft = (sampleRate * (0.2 + Random.nextDouble() * 2.5 * (1.1 - k))).toInt()
        } else {
            inBurst = true
            segmentLeft = (sampleRate * (0.1 + Random.nextDouble() * 1.2 * k)).toInt()
            cur = if (mode == NoiseMode.MIXED)
                listOf(NoiseMode.STATIC, NoiseMode.CRACKLE, NoiseMode.GLITCH).random()
            else mode
        }
    }

    private fun fill(out: ShortArray) {
        val k = intensity.coerceIn(0f, 1f)
        for (i in out.indices) {
            val forced = forcedBurstSamples > 0
            if (forced) { forcedBurstSamples--; inBurst = true; cur = if (mode == NoiseMode.MIXED) NoiseMode.STATIC else mode }
            else {
                if (segmentLeft <= 0) nextSegment()
                segmentLeft--
            }
            var s = 0f
            if (inBurst || forced) {
                val white = Random.nextFloat() * 2f - 1f
                s = when (cur) {
                    NoiseMode.STATIC, NoiseMode.MIXED -> {
                        lp += 0.6f * (white - lp); lp * 0.9f
                    }
                    NoiseMode.CRACKLE -> {
                        if (Random.nextFloat() < 0.004f + 0.02f * k) white * 1.2f else white * 0.03f
                    }
                    NoiseMode.GLITCH -> {
                        if (holdCount <= 0) { held = white; holdCount = Random.nextInt(8, 60) }
                        holdCount--
                        // Voz "robótica": onda cuadrada rápida con ruido de fondo
                        (if (held > 0) 0.5f else -0.5f) * (if ((i / 40) % 3 == 0) 0f else 1f) + white * 0.15f
                    }
                }
                s *= (0.25f + 0.75f * k)
            }
            out[i] = (s.coerceIn(-1f, 1f) * 32000).toInt().toShort()
        }
    }
}
