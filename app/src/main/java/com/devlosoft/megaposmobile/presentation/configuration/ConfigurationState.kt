package com.devlosoft.megaposmobile.presentation.configuration

import com.devlosoft.megaposmobile.core.common.ApiConfig

data class ConfigurationState(
    val serverHost: String = "",
    val useHttps: Boolean = false,
    val hostname: String = "",
    val androidId: String = "",
    val wifiIp: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val gatewayPort: Int = ApiConfig.GATEWAY_PORT
)
