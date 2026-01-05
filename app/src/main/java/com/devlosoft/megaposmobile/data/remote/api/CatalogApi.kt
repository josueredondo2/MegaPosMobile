package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.CatalogItemDto
import com.devlosoft.megaposmobile.data.remote.dto.CatalogTypeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CatalogApi {

    @GET("catalog/types")
    suspend fun getCatalogTypes(): Response<List<CatalogTypeDto>>

    @GET("catalog/items")
    suspend fun getCatalogItems(
        @Query("catalogTypeId") catalogTypeId: Int,
        @Query("filterType") filterType: String? = null,
        @Query("text") text: String? = null,
        @Query("letter") letter: String? = null,
        @Query("topN") topN: Int? = null
    ): Response<List<CatalogItemDto>>
}
