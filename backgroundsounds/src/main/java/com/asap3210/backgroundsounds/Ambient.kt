package com.asap3210.backgroundsounds

import android.content.Context
import androidx.annotation.StringRes
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** raw = nombre del archivo en res/raw (grabación real). Si es null, se sintetiza. */
enum class Ambient(@StringRes val labelRes: Int, val emoji: String, val raw: String? = null) {
    NONE(R.string.sound_none, "🔇"),
    SEA(R.string.sound_sea, "🌊", "ambient_mar"),
    BEACH(R.string.sound_beach, "🏖️", "ambient_playa"),
    GULLS(R.string.sound_gulls, "🐦", "ambient_gaviotas"),
    RAIN(R.string.sound_rain, "🌧️", "ambient_lluvia"),
    WIND(R.string.sound_wind, "💨", "ambient_viento"),
    RIVER(R.string.sound_river, "🏞️", "ambient_rio"),
    FOREST(R.string.sound_forest, "🌲", "ambient_bosque"),
    FROGS(R.string.sound_frogs, "🐸", "ambient_ranas"),
    CAMPFIRE(R.string.sound_campfire, "🏕️", "ambient_hoguera"),
    FIRE(R.string.sound_fire, "🔥", "ambient_fuego"),
    TRAFFIC(R.string.sound_traffic, "🚗", "ambient_trafico"),
    STORE(R.string.sound_store, "🛍️", "ambient_tienda"),
    FARM(R.string.sound_farm, "🐔", "ambient_granja"),
    AC(R.string.sound_ac, "❄️", "ambient_aire"),
    RITUAL(R.string.sound_ritual, "🕯️", "ambient_ritual"),
    MYSTERY(R.string.sound_mystery, "🌫️", "ambient_misterio"),
    CAFE(R.string.sound_cafe, "☕"),
    CHURCH(R.string.sound_church, "⛪"),
    AIRPORT(R.string.sound_airport, "✈️"),
    TENNIS(R.string.sound_tennis, "🎾"),
    FOOTBALL(R.string.sound_football, "⚽")
}

/** Filtro paso bajo normalizado para ruido blanco (salida ~ RMS 1). */
private class Lp(private val a: Float) {
    private var y = 0f
    private val g = (1.0 / sqrt((1.0 / 3.0) * a / (2 - a))).toFloat()
    fun p(x: Float): Float { y += a * (x - y); return y * g }
}

/** Filtro paso alto simple. */
private class Hp(private val a: Float) {
    private var y = 0f
    fun p(x: Float): Float { y += a * (x - y); return x - y }
}

/** Dispara de vez en cuando, en intervalos aleatorios. */
private class Every(private val rate: Int, private val minS: Float, private val maxS: Float) {
    private var left = (rate * (0.3f + Random.nextFloat() * maxS)).toInt()
    fun fire(): Boolean {
        if (--left <= 0) {
            left = (rate * (minS + Random.nextFloat() * (maxS - minS))).toInt().coerceAtLeast(1)
            return true
        }
        return false
    }
}

/** Envolvente que sube y baja (coche que pasa, rugido de la grada...). */
private class Surge(private val rate: Int) {
    var env = 0f
    private var rising = false
    private var step = 0f
    private var decay = 0.9999f
    fun trigger(attackS: Float, decayS: Float) {
        rising = true
        step = 1f / (rate * attackS)
        decay = exp(-1.0 / (rate * decayS)).toFloat()
    }
    fun next(): Float {
        if (rising) { env += step; if (env >= 1f) { env = 1f; rising = false } } else env *= decay
        return env
    }
}

/** Tono con caída (golpe de pelota, campana, tintineo). */
private class Ping(private val rate: Int) {
    private var env = 0f
    private var freq = 1000f
    private var ph = 0.0
    private var decay = 0.999f
    fun trigger(f: Float, decayS: Float, level: Float) {
        freq = f; env = level; decay = exp(-1.0 / (rate * decayS)).toFloat()
    }
    fun next(): Float {
        ph += 2 * PI * freq / rate
        val v = sin(ph).toFloat() * env
        env *= decay
        return v
    }
}

/**
 * Genera sonidos ambiente de forma sintética (sin archivos de audio), muestra a muestra.
 * Devuelve valores aproximadamente en [-1, 1].
 */
class AmbientGenerator(private val kind: Ambient, private val rate: Int) {
    private var n = 0L
    private fun noise() = Random.nextFloat() * 2f - 1f
    private fun sinT(freq: Double, t: Double, phase: Double = 0.0) = sin(2 * PI * freq * t + phase).toFloat()

    private val lpA = Lp(0.02f); private val lpB = Lp(0.25f); private val lpC = Lp(0.1f)
    private val lpD = Lp(0.05f); private val lpE = Lp(0.18f); private val lpF = Lp(0.03f)
    private val hpA = Hp(0.3f); private val hpB = Hp(0.2f)
    private val ev1 = Every(rate, 3f, 7f); private val ev2 = Every(rate, 12f, 30f)
    private val ev3 = Every(rate, 5f, 14f); private val ev4 = Every(rate, 1.5f, 4f)
    private val ev5 = Every(rate, 0.04f, 0.2f)
    private val s1 = Surge(rate); private val s2 = Surge(rate); private val s3 = Surge(rate)
    private val p1 = Ping(rate); private val p2 = Ping(rate)

    // Estado específico
    private var honkLeft = 0; private var honkF = 440f
    private var whistleLeft = 0
    private var hitsLeft = 0; private var hitCountdown = rate; private var hitNoise = 0f
    private var chimeLeft = 0; private var voiceLeft = 0
    private val chords = arrayOf(
        floatArrayOf(130.8f, 164.8f, 196.0f), floatArrayOf(174.6f, 220.0f, 261.6f),
        floatArrayOf(196.0f, 246.9f, 293.7f), floatArrayOf(220.0f, 261.6f, 329.6f)
    )

    fun next(): Float {
        val t = n / rate.toDouble()
        n++
        val s = when (kind) {
            Ambient.CAFE -> cafe(t)
            Ambient.CHURCH -> church(t)
            Ambient.AIRPORT -> airport(t)
            Ambient.TENNIS -> tennis(t)
            Ambient.FOOTBALL -> football(t)
            else -> 0f
        }
        return s / (1f + abs(s))
    }

    private fun sea(t: Double): Float {
        val sw = 0.6f * (0.5f + 0.5f * sinT(1 / 7.5, t)) + 0.4f * (0.5f + 0.5f * sinT(1 / 11.3, t, 1.7))
        val amp = sw * sw
        return lpA.p(noise()) * 0.18f * (0.4f + amp) + lpB.p(noise()) * 0.16f * amp
    }

    private fun rain(t: Double): Float {
        var v = hpA.p(noise()) * 0.30f * (0.85f + 0.15f * sinT(1 / 6.0, t)) + lpF.p(noise()) * 0.10f
        if (ev5.fire()) p1.trigger(1500f + Random.nextFloat() * 1800f, 0.01f, 0.25f)
        v += p1.next()
        return v
    }

    private fun cafe(t: Double): Float {
        val syl = 0.6f + 0.4f * sinT(3.7, t)
        val slow = 0.6f + 0.4f * sinT(0.31, t)
        var v = lpE.p(noise()) * 0.17f * syl * slow
        if (ev4.fire()) p1.trigger(2200f + Random.nextFloat() * 2500f, 0.12f, 0.30f)
        if (ev1.fire()) p2.trigger(900f + Random.nextFloat() * 500f, 0.08f, 0.2f)
        if (ev2.fire()) s1.trigger(0.3f, 1.5f)
        v += p1.next() + p2.next() + hpB.p(noise()) * 0.12f * s1.next()
        return v
    }

    private fun traffic(t: Double): Float {
        var v = lpF.p(noise()) * 0.22f
        if (ev1.fire()) s1.trigger(1.2f, 1.5f)
        v += lpC.p(noise()) * 0.28f * s1.next()
        if (ev2.fire()) { honkLeft = (rate * 0.35f).toInt(); honkF = if (Random.nextBoolean()) 420f else 510f }
        if (honkLeft > 0) {
            honkLeft--
            v += (sinT(honkF.toDouble(), t) + 0.5f * sinT(honkF * 1.26, t)) * 0.07f
        }
        return v
    }

    private fun church(t: Double): Float {
        var v = lpD.p(noise()) * 0.09f * (0.6f + 0.4f * sinT(0.2, t))
        val block = 8.0
        val idx = ((t / block).toInt()) % chords.size
        val frac = ((t % block) / block).toFloat()
        val g = minOf(1f, 4f * frac, 4f * (1f - frac))
        var organ = 0f
        for (f in chords[idx]) organ += sinT(f.toDouble(), t) + 0.4f * sinT(f * 2.0, t)
        v += organ * 0.04f * g * (1f + 0.1f * sinT(5.5, t))
        if (ev2.fire()) { p1.trigger(523f, 2.5f, 0.2f); p2.trigger(1318f, 1.8f, 0.1f) }
        return v + p1.next() + p2.next()
    }

    private fun airport(t: Double): Float {
        var v = lpC.p(noise()) * 0.15f * (0.7f + 0.3f * sinT(1 / 6.5, t))
        if (ev3.fire()) s1.trigger(2.0f, 4.0f)
        v += lpF.p(noise()) * 0.25f * s1.next() * (0.6f + 0.4f * sinT(14.0, t))
        if (ev2.fire()) { p1.trigger(659f, 0.5f, 0.3f); chimeLeft = (rate * 0.5f).toInt(); voiceLeft = (rate * 1.2f).toInt() }
        if (chimeLeft > 0 && --chimeLeft == 0) p2.trigger(523f, 0.7f, 0.3f)
        if (voiceLeft > 0 && --voiceLeft == 0) s2.trigger(0.3f, 2.5f)
        val vo = lpE.p(noise()) * (0.5f + 0.5f * sinT(4.2, t) * sinT(1.3, t)) * 0.30f * s2.next()
        return v + p1.next() + p2.next() + vo
    }

    private fun tennis(t: Double): Float {
        var v = lpC.p(noise()) * 0.05f
        if (--hitCountdown <= 0) {
            if (hitsLeft > 0) {
                p1.trigger(if (hitsLeft % 2 == 0) 760f else 880f, 0.03f, 0.55f)
                hitNoise = 0.5f
                hitsLeft--
                hitCountdown = (rate * (0.85f + Random.nextFloat() * 0.5f)).toInt()
            } else {
                s1.trigger(0.5f, 2.2f)
                hitsLeft = Random.nextInt(4, 10)
                hitCountdown = (rate * 3.5f).toInt()
            }
        }
        hitNoise *= 0.995f
        v += p1.next() + noise() * hitNoise * 0.2f
        v += hpA.p(noise()) * 0.35f * s1.next()
        return v
    }

    private fun football(t: Double): Float {
        var crowd = lpE.p(noise()) * 0.13f * (1f + 0.3f * sinT(1 / 3.1, t))
        if (ev3.fire()) s1.trigger(1.2f, 3.5f)
        crowd *= 1f + s1.next() * 3f
        if (ev2.fire()) whistleLeft = (rate * 0.7f).toInt()
        var v = crowd
        if (whistleLeft > 0) {
            whistleLeft--
            v += sin(2 * PI * 2900 * t + 6 * sin(2 * PI * 28 * t)).toFloat() * 0.05f
        }
        return v
    }
}

/** Reproduce un sonido ambiente en bucle (grabación real o sintetizado). */
class AmbientEngine(private val ctx: Context, private val usage: Int) {
    private val rate = 16000
    @Volatile private var volume = 0.5f
    @Volatile private var running = false
    private var thread: Thread? = null
    private var player: MediaPlayer? = null

    val isRunning get() = running

    fun play(kind: Ambient, vol: Float) {
        stop()
        if (kind == Ambient.NONE) return
        volume = vol
        running = true
        val raw = kind.raw
        if (raw != null) startRaw(raw) else startSynth(kind)
    }

    fun setVolume(v: Float) {
        volume = v
        player?.setVolume(v, v)
    }

    private fun startRaw(name: String) {
        val id = ctx.resources.getIdentifier(name, "raw", ctx.packageName)
        val attrs = AudioAttributes.Builder().setUsage(usage).build()
        val session = (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
        val mp = if (id != 0) MediaPlayer.create(ctx, id, attrs, session) else null
        if (mp == null) { running = false; return }
        mp.isLooping = true
        mp.setVolume(volume, volume)
        mp.start()
        player = mp
    }

    private fun startSynth(kind: Ambient) {
        thread = Thread {
            val minBuf = AudioTrack.getMinBufferSize(
                rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            val buf = ShortArray(1024)
            val gen = AmbientGenerator(kind, rate)
            while (running) {
                val vol = volume.coerceIn(0f, 1f)
                for (i in buf.indices) buf[i] = (gen.next() * vol * 30000f).toInt().toShort()
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
        player?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        player = null
    }
}
