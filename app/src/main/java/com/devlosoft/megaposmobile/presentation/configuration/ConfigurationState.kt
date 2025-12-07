package com.devlosoft.megaposmobile.presentation.configuration

data class ConfigurationState(
    val serverUrl: String = "",
    val hostname: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)
