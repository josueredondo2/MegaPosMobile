package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.ProcessPermission
import com.devlosoft.megaposmobile.domain.model.ScreenAccess
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.google.gson.annotations.SerializedName

data class UserPermissionsDto(
    @SerializedName("groupCode")
    val groupCode: String?,

    @SerializedName("groupName")
    val groupName: String?,

    @SerializedName("processes")
    val processes: Map<String, ProcessPermissionDto>?,

    @SerializedName("screensAccess")
    val screensAccess: List<ScreenAccessDto>?
) {
    fun toDomain(): UserPermissions = UserPermissions(
        groupCode = groupCode ?: "",
        groupName = groupName ?: "",
        processes = processes?.mapValues { it.value.toDomain() } ?: emptyMap(),
        screensAccess = screensAccess?.map { it.toDomain() } ?: emptyList()
    )
}

data class ProcessPermissionDto(
    @SerializedName("access")
    val access: Boolean?,

    @SerializedName("show")
    val show: Boolean?
) {
    fun toDomain(): ProcessPermission = ProcessPermission(
        access = access ?: false,
        show = show ?: false
    )
}

data class ScreenAccessDto(
    @SerializedName("systemCode")
    val systemCode: Int?,

    @SerializedName("groupCode")
    val groupCode: String?,

    @SerializedName("screenAlias")
    val screenAlias: String?,

    @SerializedName("screenEnter")
    val screenEnter: Int?,

    @SerializedName("screenInclude")
    val screenInclude: Int?,

    @SerializedName("screenCopy")
    val screenCopy: Int?,

    @SerializedName("screenModify")
    val screenModify: Int?,

    @SerializedName("screenDelete")
    val screenDelete: Int?,

    @SerializedName("screenNavigate")
    val screenNavigate: Int?,

    @SerializedName("screenSearch")
    val screenSearch: Int?,

    @SerializedName("screenPrint")
    val screenPrint: Int?,

    @SerializedName("screenRefresh")
    val screenRefresh: Int?,

    @SerializedName("screenHelp")
    val screenHelp: Int?,

    @SerializedName("screenQuery")
    val screenQuery: Int?
) {
    fun toDomain(): ScreenAccess = ScreenAccess(
        systemCode = systemCode ?: 0,
        groupCode = groupCode ?: "",
        screenAlias = screenAlias ?: "",
        screenEnter = screenEnter ?: 0,
        screenInclude = screenInclude ?: 0,
        screenCopy = screenCopy ?: 0,
        screenModify = screenModify ?: 0,
        screenDelete = screenDelete ?: 0,
        screenNavigate = screenNavigate ?: 0,
        screenSearch = screenSearch ?: 0,
        screenPrint = screenPrint ?: 0,
        screenRefresh = screenRefresh ?: 0,
        screenHelp = screenHelp ?: 0,
        screenQuery = screenQuery ?: 0
    )
}
