package com.devlosoft.megaposmobile.data.remote.interceptor

import com.devlosoft.megaposmobile.core.util.NetworkUtils
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
    private val serverConfigDao: ServerConfigDao,
    private val networkUtils: NetworkUtils
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get server config from database (required)
        val serverConfig = runBlocking {
            serverConfigDao.getActiveServerConfigSync()
        } ?: throw IOException("Configuración del servidor no encontrada. Por favor configure la URL y el hostname en Configuración.")

        // Get hostname from database config (required)
        val hostname = serverConfig.serverName.takeIf { it.isNotBlank() }
            ?: throw IOException("Hostname no configurado. Por favor configure el hostname en Configuración.")

        // Get base URL from database config (required)
        val configuredBaseUrl = serverConfig.serverUrl.takeIf { it.isNotBlank() }?.toHttpUrlOrNull()
            ?: throw IOException("URL del servidor no configurada o inválida. Por favor configure la URL en Configuración.")

        // Build new URL using configured base URL from database
        val newUrl = originalRequest.url.newBuilder()
            .scheme(configuredBaseUrl.scheme)
            .host(configuredBaseUrl.host)
            .port(configuredBaseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        // Get device WiFi IP address
        val deviceIp = networkUtils.getWifiIpAddress()

        // Skip auth header for login endpoint
        if (newRequest.url.encodedPath.endsWith("login")) {
            val requestWithHostname = newRequest.newBuilder()
                .header("x-Hostname", hostname)
                .header("x-Device-IP", deviceIp)
                .build()
            return chain.proceed(requestWithHostname)
        }

        val token = runBlocking {
            sessionManager.getAccessToken().first()
        }

        val request = if (!token.isNullOrEmpty()) {
            newRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("x-Hostname", hostname)
                .header("x-Device-IP", deviceIp)
                .build()
        } else {
            newRequest.newBuilder()
                .header("x-Hostname", hostname)
                .header("x-Device-IP", deviceIp)
                .build()
        }

        return chain.proceed(request)
    }
}
