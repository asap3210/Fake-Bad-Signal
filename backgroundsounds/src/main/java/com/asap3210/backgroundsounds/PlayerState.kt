package com.asap3210.backgroundsounds

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Estado compartido entre el servicio y la pantalla (mismo proceso). */
object PlayerState {
    var playing by mutableStateOf<Ambient?>(null)
}
