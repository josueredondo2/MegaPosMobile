package com.devlosoft.megaposmobile.core.state

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado global del datáfono.
 * Mantiene el terminal ID en memoria y se sincroniza con la base de datos.
 */
@Singleton
class DataphoneState @Inject constructor() {

    companion object {
        private const val TAG = "DataphoneState"
    }

    private val _terminalId = MutableStateFlow("")
    val terminalId: StateFlow<String> = _terminalId.asStateFlow()

    /**
     * Establece el terminal ID.
     * @param id El nuevo terminal ID
     */
    fun setTerminalId(id: String) {
        val oldId = _terminalId.value
        _terminalId.value = id
        if (oldId != id) {
            Log.d(TAG, "Terminal ID changed: '$oldId' -> '$id'")
        }
    }

    /**
     * Obtiene el terminal ID actual.
     * @return El terminal ID o cadena vacía si no está configurado
     */
    fun getTerminalId(): String = _terminalId.value

    /**
     * Verifica si el terminal ID está configurado.
     * @return true si hay un terminal ID válido
     */
    fun hasTerminalId(): Boolean = _terminalId.value.isNotBlank()
}
