package com.devlosoft.megaposmobile.data.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.api.CatalogApi
import com.devlosoft.megaposmobile.domain.model.CatalogItem
import com.devlosoft.megaposmobile.domain.model.CatalogType
import com.devlosoft.megaposmobile.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val catalogApi: CatalogApi
) : CatalogRepository {

    override suspend fun getCatalogTypes(): Flow<Resource<List<CatalogType>>> = flow {
        emit(Resource.Loading())
        try {
            val response = catalogApi.getCatalogTypes()
            if (response.isSuccessful) {
                val types = response.body()?.map { dto ->
                    CatalogType(
                        catalogTypeId = dto.catalogTypeId,
                        catalogName = dto.catalogName
                    )
                } ?: emptyList()
                emit(Resource.Success(types))
            } else {
                emit(Resource.Error("Error al cargar categorías: ${response.code()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }

    override suspend fun getCatalogItems(
        catalogTypeId: Int,
        filterType: String?,
        text: String?,
        letter: String?,
        topN: Int?
    ): Flow<Resource<List<CatalogItem>>> = flow {
        emit(Resource.Loading())
        try {
            val response = catalogApi.getCatalogItems(
                catalogTypeId = catalogTypeId,
                filterType = filterType,
                text = text,
                letter = letter,
                topN = topN
            )
            if (response.isSuccessful) {
                val items = response.body()?.map { dto ->
                    CatalogItem(
                        unitSalesType = dto.unitSalesType,
                        catalogItemName = dto.catalogItemName,
                        catalogItemImage = dto.catalogItemImage,
                        itemPosId = dto.itemPosId
                    )
                } ?: emptyList()
                emit(Resource.Success(items))
            } else {
                emit(Resource.Error("Error al cargar productos: ${response.code()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }
}
