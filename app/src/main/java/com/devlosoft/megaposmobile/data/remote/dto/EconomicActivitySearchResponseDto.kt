package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EconomicActivitySearchResponseDto(
    @SerializedName("data")
    val data: List<EconomicActivitySearchItemDto>,

    @SerializedName("pageNumber")
    val pageNumber: Int,

    @SerializedName("pageSize")
    val pageSize: Int,

    @SerializedName("totalRecords")
    val totalRecords: Int,

    @SerializedName("totalPages")
    val totalPages: Int,

    @SerializedName("hasNextPage")
    val hasNextPage: Boolean,

    @SerializedName("hasPreviousPage")
    val hasPreviousPage: Boolean
)

data class EconomicActivitySearchItemDto(
    @SerializedName("code")
    val code: String,

    @SerializedName("description")
    val description: String
)
