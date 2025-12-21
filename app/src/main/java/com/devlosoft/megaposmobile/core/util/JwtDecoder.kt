package com.devlosoft.megaposmobile.core.util

import android.util.Base64
import org.json.JSONObject

object JwtDecoder {

    data class JwtPayload(
        val userName: String?,
        val userCode: String?,
        val sessionId: String?,
        val businessUnitName: String?,
        val exp: Long?,
        val iat: Long?
    )

    fun decode(token: String): JwtPayload? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return null
            }

            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            val decodedPayload = String(decodedBytes, Charsets.UTF_8)
            val jsonObject = JSONObject(decodedPayload)

            JwtPayload(
                userName = if (jsonObject.has("UserName")) jsonObject.getString("UserName") else null,
                userCode = if (jsonObject.has("UserCode")) jsonObject.getString("UserCode") else null,
                sessionId = if (jsonObject.has("SessionId")) jsonObject.getString("SessionId") else null,
                businessUnitName = if (jsonObject.has("BusinessUnitName")) jsonObject.getString("BusinessUnitName") else null,
                exp = if (jsonObject.has("exp")) jsonObject.getLong("exp") else null,
                iat = if (jsonObject.has("iat")) jsonObject.getLong("iat") else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
