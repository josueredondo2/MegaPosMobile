package com.devlosoft.megaposmobile.domain.usecase.billing

import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for checking if the current user has permission to execute a process.
 * Returns both the permission status and the user code if they have access.
 */
class CheckPermissionUseCase @Inject constructor(
    private val sessionManager: SessionManager
) {
    /**
     * Result of a permission check
     * @param hasAccess true if user has permission
     * @param userCode the user's code if they have access (for authorization tracking)
     */
    data class PermissionResult(
        val hasAccess: Boolean,
        val userCode: String? = null
    )

    /**
     * Checks if the user has permission to execute the specified process.
     *
     * @param permissions The user's permissions object
     * @param processCode The process code to check access for
     * @return PermissionResult with access status and user code if accessible
     */
    suspend operator fun invoke(
        permissions: UserPermissions?,
        processCode: String
    ): PermissionResult {
        val hasAccess = permissions?.hasAccess(processCode) ?: false

        return if (hasAccess) {
            val userCode = sessionManager.getUserCode().first()
            PermissionResult(hasAccess = true, userCode = userCode)
        } else {
            PermissionResult(hasAccess = false, userCode = null)
        }
    }
}
