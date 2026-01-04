package com.devlosoft.megaposmobile.presentation.configuration

data class ConfigurationState(
    val serverHost: String = "",
    val useHttps: Boolean = false,
    val hostname: String = "",
    val androidId: String = "",
    val wifiIp: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
