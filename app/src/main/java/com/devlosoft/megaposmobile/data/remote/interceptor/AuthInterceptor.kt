package com.devlosoft.megaposmobile.data.remote.interceptor

import android.os.Build
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Get device hostname (using device model and manufacturer)
        val hostname = "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_")

        // Skip auth header for login endpoint
        if (originalRequest.url.encodedPath.endsWith("login")) {
            val requestWithHostname = originalRequest.newBuilder()
                .header("x-Hostname", hostname)
                .build()
            return chain.proceed(requestWithHostname)
        }

        val token = runBlocking {
            sessionManager.getAccessToken().first()
        }

        val request = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("x-Hostname", hostname)
                .build()
        } else {
            originalRequest.newBuilder()
                .header("x-Hostname", hostname)
                .build()
        }

        return chain.proceed(request)
    }
}
