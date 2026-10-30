package com.asap3210.fakebadsignal

import android.content.Context

/** Ajustes guardados: contactos elegidos, activación automática e intensidad. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("fakebadsignal", Context.MODE_PRIVATE)

    /** Clave de comparación: los últimos 9 dígitos, para ignorar prefijos (+34, 0034...). */
    fun key(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    fun selected(c: Context): Set<String> = sp(c).getStringSet("selected", emptySet())!!.toSet()
    fun setSelected(c: Context, v: Set<String>) = sp(c).edit().putStringSet("selected", v).apply()

    fun auto(c: Context) = sp(c).getBoolean("auto", true)
    fun setAuto(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto", v).apply()

    fun intensity(c: Context) = sp(c).getFloat("intensity", 0.6f)
    fun setIntensity(c: Context, v: Float) = sp(c).edit().putFloat("intensity", v).apply()

    fun noise(c: Context) = sp(c).getBoolean("noise", false)
    fun setNoise(c: Context, v: Boolean) = sp(c).edit().putBoolean("noise", v).apply()
}
