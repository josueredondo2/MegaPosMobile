package com.devlosoft.megaposmobile.core.common

/**
 * Configuracion del API Gateway.
 * Centraliza constantes y utilidades para la construccion de URLs.
 */
object ApiConfig {
    const val DEFAULT_SCHEME = "http"
    const val HTTPS_SCHEME = "https"

    // Base paths de los APIs disponibles
    const val POS_API_BASE_PATH = "pos-api/v1"
    const val FEL_API_BASE_PATH = "fel-api/v1"

    /**
     * Construye la URL completa del gateway a partir del host.
     * @param host IP:puerto o dominio:puerto del servidor (ej: "192.168.1.100:5166")
     * @param useHttps true para HTTPS, false para HTTP
     * @return URL completa del gateway (ej: "http://192.168.1.100:5166")
     */
    fun buildGatewayUrl(host: String, useHttps: Boolean = false): String {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/")

        val scheme = if (useHttps) HTTPS_SCHEME else DEFAULT_SCHEME
        return "$scheme://$cleanHost"
    }

    /**
     * Extrae el host:puerto de una URL completa.
     * @param url URL completa (ej: "http://192.168.1.100:5166")
     * @return Host con puerto (ej: "192.168.1.100:5166")
     */
    fun extractHostFromUrl(url: String): String {
        return url.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/")
    }

    /**
     * Determina si una URL usa HTTPS.
     * @param url URL completa
     * @return true si usa HTTPS
     */
    fun isHttps(url: String): Boolean {
        return url.trim().lowercase().startsWith("https://")
    }

    /**
     * Valida si un string es una IP:puerto o dominio:puerto valido.
     * @param host IP:puerto o dominio:puerto a validar (ej: "192.168.1.100:5166")
     * @return true si es valido
     */
    fun isValidHost(host: String): Boolean {
        if (host.isBlank()) return false

        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/")

        // Separar host y puerto
        val parts = cleanHost.split(":")
        val hostPart = parts.firstOrNull() ?: return false
        val portPart = parts.getOrNull(1)

        // Validar puerto si existe
        if (portPart != null) {
            val port = portPart.toIntOrNull() ?: return false
            if (port !in 1..65535) return false
        }

        // Patron para IP
        val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (ipPattern.matches(hostPart)) {
            // Validar que cada octeto sea <= 255
            return hostPart.split(".").all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
        }

        // Patron para dominio (permite subdominios, letras, numeros, guiones)
        val domainPattern = Regex("""^[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?)*$""")
        return domainPattern.matches(hostPart)
    }
}

/**
 * Enum que representa los backends API disponibles a traves del gateway.
 */
enum class ApiBackend(val basePath: String) {
    POS_API(ApiConfig.POS_API_BASE_PATH),
    FEL_API(ApiConfig.FEL_API_BASE_PATH);

    /**
     * Retorna el path base con slash final para Retrofit.
     */
    fun getBasePathWithSlash(): String = "$basePath/"
}
