package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.CatalogItem
import com.devlosoft.megaposmobile.domain.model.CatalogType
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {

    suspend fun getCatalogTypes(): Flow<Resource<List<CatalogType>>>

    suspend fun getCatalogItems(
        catalogTypeId: Int,
        filterType: String? = null,
        text: String? = null,
        letter: String? = null,
        topN: Int? = null
    ): Flow<Resource<List<CatalogItem>>>
}
