package com.devlosoft.megaposmobile.domain.model

/**
 * Proveedores de datáfono soportados
 */
enum class DatafonoProvider(val displayName: String, val defaultPort: Int) {
    PAX_BAC("PAX - BAC Credomatic", 8080);
    // Futuro: INGENICO("Ingenico", 9090), VERIFONE("Verifone", 8088)

    companion object {
        fun fromString(value: String): DatafonoProvider {
            // Mantener compatibilidad con valores anteriores
            return when (value) {
                "BAC" -> PAX_BAC
                else -> entries.find { it.name == value } ?: PAX_BAC
            }
        }
    }
}
