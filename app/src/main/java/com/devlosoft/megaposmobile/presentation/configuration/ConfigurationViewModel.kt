package com.devlosoft.megaposmobile.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.ApiConfig
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
                            serverHost = ApiConfig.extractHostFromUrl(config.serverUrl),
                            useHttps = config.useHttps,
                            hostname = config.serverName
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ConfigurationEvent) {
        when (event) {
            is ConfigurationEvent.ServerHostChanged -> {
                _state.update { it.copy(serverHost = event.host) }
            }
            is ConfigurationEvent.UseHttpsChanged -> {
                _state.update { it.copy(useHttps = event.useHttps) }
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

        if (currentState.serverHost.isBlank()) {
            _state.update { it.copy(error = "La IP o dominio del servidor es requerida") }
            return
        }

        if (!ApiConfig.isValidHost(currentState.serverHost)) {
            _state.update { it.copy(error = "Formato de IP o dominio invalido") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val fullUrl = ApiConfig.buildGatewayUrl(currentState.serverHost, currentState.useHttps)

                // Check if config already exists
                val existingConfig = serverConfigDao.getActiveServerConfigSync()

                if (existingConfig != null) {
                    // Update URL, hostname and HTTPS setting
                    serverConfigDao.updateServerConfig(
                        serverUrl = fullUrl,
                        serverName = currentState.hostname,
                        useHttps = currentState.useHttps
                    )
                } else {
                    // Create new config if none exists
                    val config = ServerConfigEntity(
                        id = 1,
                        serverUrl = fullUrl,
                        serverName = currentState.hostname,
                        isActive = true,
                        useHttps = currentState.useHttps
                    )
                    serverConfigDao.insertServerConfig(config)
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al guardar la configuracion: ${e.message}"
                    )
                }
            }
        }
    }
}
