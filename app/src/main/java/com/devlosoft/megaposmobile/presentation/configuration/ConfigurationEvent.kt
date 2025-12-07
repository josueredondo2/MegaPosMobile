package com.devlosoft.megaposmobile.presentation.configuration

sealed class ConfigurationEvent {
    data class ServerUrlChanged(val url: String) : ConfigurationEvent()
    data class HostnameChanged(val hostname: String) : ConfigurationEvent()
    data object Save : ConfigurationEvent()
    data object ClearError : ConfigurationEvent()
    data object ClearSavedFlag : ConfigurationEvent()
}
