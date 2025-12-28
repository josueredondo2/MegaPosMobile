package com.devlosoft.megaposmobile.domain.usecase.billing

import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.domain.usecase.AuthorizeProcessUseCase
import javax.inject.Inject

/**
 * Handler for actions that may require authorization.
 * Encapsulates the logic of checking permissions and handling the authorization flow.
 */
class AuthorizedActionHandler @Inject constructor(
    private val checkPermissionUseCase: CheckPermissionUseCase,
    private val authorizeProcessUseCase: AuthorizeProcessUseCase
) {
    /**
     * Result of checking if an action requires authorization
     */
    sealed class CheckResult {
        /**
         * User has direct access - can execute immediately
         * @param userCode The user's code for tracking who executed the action
         */
        data class HasAccess(val userCode: String?) : CheckResult()

        /**
         * User needs authorization from another user
         */
        data object RequiresAuthorization : CheckResult()
    }

    /**
     * Result of submitting authorization credentials
     */
    sealed class AuthorizationResult {
        /**
         * Authorization successful
         * @param authorizedBy The code of the user who authorized
         */
        data class Success(val authorizedBy: String) : AuthorizationResult()

        /**
         * Authorization failed
         * @param message Error message describing why authorization failed
         */
        data class Failed(val message: String) : AuthorizationResult()
    }

    /**
     * Checks if the user has permission to execute an action.
     *
     * @param permissions The user's permissions object
     * @param processCode The process code to check access for
     * @return CheckResult indicating if user has access or needs authorization
     */
    suspend fun checkAccess(
        permissions: UserPermissions?,
        processCode: String
    ): CheckResult {
        val result = checkPermissionUseCase(permissions, processCode)
        return if (result.hasAccess) {
            CheckResult.HasAccess(result.userCode)
        } else {
            CheckResult.RequiresAuthorization
        }
    }

    /**
     * Submits authorization credentials to authorize an action.
     *
     * @param userCode The authorizing user's code
     * @param password The authorizing user's password
     * @param processCode The process code being authorized
     * @return AuthorizationResult with success or failure details
     */
    suspend fun submitAuthorization(
        userCode: String,
        password: String,
        processCode: String
    ): AuthorizationResult {
        return authorizeProcessUseCase(userCode, password, processCode)
            .fold(
                onSuccess = { AuthorizationResult.Success(userCode) },
                onFailure = { error ->
                    AuthorizationResult.Failed(error.message ?: "Error de autorización")
                }
            )
    }
}
