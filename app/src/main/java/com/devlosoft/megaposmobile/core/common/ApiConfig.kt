package com.devlosoft.megaposmobile.core.common

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Configuracion del API Gateway.
 * Centraliza constantes y utilidades para la construccion de URLs.
 */
object ApiConfig {
    const val GATEWAY_PORT = 5166
    const val DEFAULT_SCHEME = "http"
    const val HTTPS_SCHEME = "https"

    // Base paths de los APIs disponibles
    const val POS_API_BASE_PATH = "pos-api/v1"
    const val FEL_API_BASE_PATH = "fel-api/v1"

    /**
     * Construye la URL completa del gateway a partir del host.
     * @param host IP o dominio del servidor (ej: "192.168.1.100" o "api.empresa.com")
     * @param useHttps true para HTTPS, false para HTTP
     * @return URL completa del gateway (ej: "http://192.168.1.100:5166")
     */
    fun buildGatewayUrl(host: String, useHttps: Boolean = false): String {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/")
            .split(":").firstOrNull() ?: host // Remover puerto si existe

        val scheme = if (useHttps) HTTPS_SCHEME else DEFAULT_SCHEME
        return "$scheme://$cleanHost:$GATEWAY_PORT"
    }

    /**
     * Extrae el host (IP o dominio) de una URL completa.
     * @param url URL completa (ej: "http://192.168.1.100:5166")
     * @return Solo el host (ej: "192.168.1.100")
     */
    fun extractHostFromUrl(url: String): String {
        return try {
            url.toHttpUrlOrNull()?.host ?: url
                .removePrefix("http://")
                .removePrefix("https://")
                .split(":").firstOrNull()
                ?.removeSuffix("/")
                ?: url
        } catch (e: Exception) {
            url
        }
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
     * Valida si un string es una IP o dominio valido.
     * @param host IP o dominio a validar
     * @return true si es valido
     */
    fun isValidHost(host: String): Boolean {
        if (host.isBlank()) return false

        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .split(":").firstOrNull() ?: return false

        // Patron para IP
        val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (ipPattern.matches(cleanHost)) {
            // Validar que cada octeto sea <= 255
            return cleanHost.split(".").all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
        }

        // Patron para dominio (permite subdominios, letras, numeros, guiones)
        val domainPattern = Regex("""^[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?)*$""")
        return domainPattern.matches(cleanHost)
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
