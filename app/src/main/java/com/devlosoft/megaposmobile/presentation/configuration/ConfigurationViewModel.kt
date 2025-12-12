package com.devlosoft.megaposmobile.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.util.DeviceIdentifier
import com.devlosoft.megaposmobile.core.util.NetworkUtils
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.entity.ServerConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val serverConfigDao: ServerConfigDao,
    private val deviceIdentifier: DeviceIdentifier,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigurationState())
    val state: StateFlow<ConfigurationState> = _state.asStateFlow()

    init {
        loadConfiguration()
        loadAndroidId()
        loadWifiIp()
    }

    private fun loadAndroidId() {
        val androidId = deviceIdentifier.getDeviceId()
        _state.update { it.copy(androidId = androidId) }
    }

    private fun loadWifiIp() {
        val wifiIp = networkUtils.getWifiIpAddress()
        _state.update { it.copy(wifiIp = wifiIp) }
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            serverConfigDao.getActiveServerConfig().collect { config ->
                config?.let {
                    _state.update { currentState ->
                        currentState.copy(
                            serverUrl = config.serverUrl,
                            hostname = config.serverName
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ConfigurationEvent) {
        when (event) {
            is ConfigurationEvent.ServerUrlChanged -> {
                _state.update { it.copy(serverUrl = event.url) }
            }
            is ConfigurationEvent.HostnameChanged -> {
                _state.update { it.copy(hostname = event.hostname) }
            }
            is ConfigurationEvent.Save -> {
                saveConfiguration()
            }
            is ConfigurationEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is ConfigurationEvent.ClearSavedFlag -> {
                _state.update { it.copy(isSaved = false) }
            }
        }
    }

    private fun saveConfiguration() {
        val currentState = _state.value

        if (currentState.serverUrl.isBlank()) {
            _state.update { it.copy(error = "La URL del servidor es requerida") }
            return
        }

        if (currentState.hostname.isBlank()) {
            _state.update { it.copy(error = "El hostname es requerido") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val config = ServerConfigEntity(
                    id = 1,
                    serverUrl = currentState.serverUrl,
                    serverName = currentState.hostname,
                    isActive = true
                )
                serverConfigDao.insertServerConfig(config)
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al guardar la configuración: ${e.message}"
                    )
                }
            }
        }
    }
}
