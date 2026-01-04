package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.AppVersionDto
import retrofit2.Response
import retrofit2.http.GET

interface SystemApi {
    @GET("system/version")
    suspend fun getAppVersion(): Response<AppVersionDto>
}
