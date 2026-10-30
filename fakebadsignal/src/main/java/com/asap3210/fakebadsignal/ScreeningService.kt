package com.asap3210.fakebadsignal

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Android avisa a esta clase de cada llamada (entrante o saliente) para que sepamos el número.
 * Nunca bloquea ni modifica la llamada: solo decide si activar el efecto.
 */
class ScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart
        if (number != null && Prefs.auto(this) && Prefs.key(number) in Prefs.selected(this)) {
            try {
                startForegroundService(Intent(this, CallService::class.java))
            } catch (_: Exception) {}
        }
        respondToCall(details, CallResponse.Builder().build())
    }
}
