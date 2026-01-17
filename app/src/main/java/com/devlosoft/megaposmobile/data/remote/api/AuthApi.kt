package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.CheckSessionStatusResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.GrantProcessExecRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.LoginRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.LoginResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.UserPermissionsDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequestDto): Response<LoginResponseDto>

    @POST("logout")
    suspend fun logout(): Response<Unit>

    @GET("user-permissions")
    suspend fun getUserPermissions(): Response<UserPermissionsDto>

    @POST("grant-process-exec")
    suspend fun grantProcessExec(@Body request: GrantProcessExecRequestDto): Response<Boolean>

    @GET("session-status")
    suspend fun checkSessionStatus(): Response<CheckSessionStatusResponseDto>
}
