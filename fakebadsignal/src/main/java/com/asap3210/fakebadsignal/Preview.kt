package com.asap3210.fakebadsignal

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.random.Random

/**
 * Graba ~8 s de tu voz, le aplica los mismos cortes que se aplicarán en la llamada
 * (y el ruido, si está activado) y lo reproduce para que oigas cómo te oirá el otro.
 */
object Preview {
    private const val RATE = 16000
    private const val SECONDS = 8

    @Suppress("MissingPermission")
    fun run(ctx: Context, intensity: Float, withNoise: Boolean, onUpdate: (String, Boolean) -> Unit) {
        val ui = Handler(Looper.getMainLooper())
        fun say(m: String, done: Boolean = false) = ui.post { onUpdate(m, done) }

        Thread {
            try {
                val total = RATE * SECONDS
                val data = ShortArray(total)
                val rec = AudioRecord(
                    MediaRecorder.AudioSource.MIC, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT), 8192)
                )
                say("Grabando 8 segundos… habla con normalidad.")
                rec.startRecording()
                var off = 0
                while (off < total) {
                    val n = rec.read(data, off, minOf(1024, total - off))
                    if (n <= 0) break
                    off += n
                }
                rec.stop(); rec.release()

                say("Aplicando la interferencia…")
                val k = intensity.coerceIn(0.05f, 1f)
                var i = 0
                var lp = 0f
                while (i < total) {
                    i += (CallEffect.talkMs(k) * RATE / 1000).toInt()   // voz normal
                    val cut = (CallEffect.cutMs(k) * RATE / 1000).toInt()
                    val end = minOf(total, i + cut)
                    while (i < end) {
                        val white = Random.nextFloat() * 2f - 1f
                        lp += 0.6f * (white - lp)
                        data[i] = if (withNoise) (lp * 9000 * (0.25f + 0.75f * k)).toInt().toShort() else 0
                        i++
                    }
                }

                say("Reproduciendo: así te oirá la otra persona.")
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setBufferSizeInBytes(total * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(data, 0, total)
                track.play()
                Thread.sleep(SECONDS * 1000L + 300)
                track.stop(); track.release()
                say("Listo. Cambia la intensidad y vuelve a probar si quieres.", true)
            } catch (e: Exception) {
                say("No se pudo grabar: ${e.message}", true)
            }
        }.start()
    }
}
