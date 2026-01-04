package com.devlosoft.megaposmobile.presentation.configuration

sealed class ConfigurationEvent {
    data class ServerHostChanged(val host: String) : ConfigurationEvent()
    data class UseHttpsChanged(val useHttps: Boolean) : ConfigurationEvent()
    data class HostnameChanged(val hostname: String) : ConfigurationEvent()
    data object Save : ConfigurationEvent()
    data object ClearError : ConfigurationEvent()
    data object ClearSavedFlag : ConfigurationEvent()
}
