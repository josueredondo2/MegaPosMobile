package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.AppVersion
import com.google.gson.annotations.SerializedName

data class AppVersionDto(
    @SerializedName("systemCode")
    val systemCode: Int,
    @SerializedName("libraryName")
    val libraryName: String,
    @SerializedName("version")
    val version: String,
    @SerializedName("major")
    val major: Int,
    @SerializedName("minor")
    val minor: Int,
    @SerializedName("build")
    val build: Int,
    @SerializedName("release")
    val release: Int,
    @SerializedName("lastChange")
    val lastChange: String?
) {
    fun toDomain(): AppVersion = AppVersion(
        version = version,
        major = major,
        minor = minor,
        build = build,
        release = release
    )
}
