package com.devlosoft.megaposmobile.data.repository

import android.util.Log
import com.devlosoft.megaposmobile.data.remote.api.AuditApi
import com.devlosoft.megaposmobile.data.remote.dto.AuditLogRequestDto
import com.devlosoft.megaposmobile.domain.repository.AuditRepository
import javax.inject.Inject

class AuditRepositoryImpl @Inject constructor(
    private val auditApi: AuditApi
) : AuditRepository {

    companion object {
        private const val TAG = "AuditRepository"
    }

    override suspend fun log(action: String, detail: String, transactionId: String?) {
        try {
            val request = AuditLogRequestDto(
                action = action,
                detail = detail,
                transactionId = transactionId
            )

            val response = auditApi.log(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Audit log sent successfully: $detail")
            } else {
                Log.w(TAG, "Failed to send audit log: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audit log: ${e.message}")
        }
    }
}
