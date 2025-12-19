package com.devlosoft.megaposmobile.domain.model

/**
 * Proveedores de datafono soportados
 */
enum class DatafonoProvider(val displayName: String) {
    BAC("BAC");

    companion object {
        fun fromString(value: String): DatafonoProvider {
            return entries.find { it.name == value } ?: BAC
        }
    }
}
