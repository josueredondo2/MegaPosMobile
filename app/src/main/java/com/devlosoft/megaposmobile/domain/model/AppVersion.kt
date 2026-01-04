package com.devlosoft.megaposmobile.domain.model

data class AppVersion(
    val version: String,
    val major: Int,
    val minor: Int,
    val build: Int,
    val release: Int
)
