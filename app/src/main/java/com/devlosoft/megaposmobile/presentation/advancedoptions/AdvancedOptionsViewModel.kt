package com.devlosoft.megaposmobile.presentation.advancedoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.core.util.BluetoothPrinterDevice
import com.devlosoft.megaposmobile.core.util.BluetoothPrinterManager
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.entity.ServerConfigEntity
import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import com.devlosoft.megaposmobile.domain.model.ReaderBrand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedOptionsViewModel @Inject constructor(
    private val serverConfigDao: ServerConfigDao,
    private val bluetoothPrinterManager: BluetoothPrinterManager,
    private val printerManager: PrinterManager
) : ViewModel() {

    private val _state = MutableStateFlow(AdvancedOptionsState())
    val state: StateFlow<AdvancedOptionsState> = _state.asStateFlow()

    init {
        loadConfiguration()
        checkBluetoothAvailability()
    }

    fun onEvent(event: AdvancedOptionsEvent) {
        when (event) {
            is AdvancedOptionsEvent.HostnameChanged -> {
                _state.update { it.copy(hostname = event.hostname) }
            }
            is AdvancedOptionsEvent.DatafonUrlChanged -> {
                _state.update { it.copy(datafonUrl = event.datafonUrl) }
            }
            is AdvancedOptionsEvent.DatafonoProviderChanged -> {
                _state.update { it.copy(datafonoProvider = event.provider) }
            }
            is AdvancedOptionsEvent.PrinterIpChanged -> {
                _state.update { it.copy(printerIp = event.printerIp) }
            }
            is AdvancedOptionsEvent.PrinterModelChanged -> {
                _state.update { it.copy(printerModel = event.model) }
            }
            is AdvancedOptionsEvent.ReaderBrandChanged -> {
                _state.update { it.copy(readerBrand = event.brand) }
            }
            is AdvancedOptionsEvent.PrinterModeChanged -> {
                _state.update { it.copy(usePrinterIp = event.useIp) }
            }
            is AdvancedOptionsEvent.BluetoothDeviceSelected -> {
                _state.update { it.copy(selectedBluetoothDevice = event.device) }
            }
            is AdvancedOptionsEvent.RefreshBluetoothDevices -> {
                loadBluetoothDevices()
            }
            is AdvancedOptionsEvent.RequestBluetoothPermissions -> {
                // Permission request handled in UI
            }
            is AdvancedOptionsEvent.OnPermissionsResult -> {
                onPermissionsResult(event.granted)
            }
            is AdvancedOptionsEvent.TestPrinter -> {
                testPrinter()
            }
            is AdvancedOptionsEvent.Save -> {
                saveConfiguration()
            }
            is AdvancedOptionsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
            is AdvancedOptionsEvent.ClearSavedFlag -> {
                _state.update { it.copy(isSaved = false) }
            }
        }
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            try {
                val config = serverConfigDao.getActiveServerConfigSync()
                if (config != null) {
                    _state.update {
                        it.copy(
                            hostname = config.serverName,
                            datafonUrl = config.datafonUrl,
                            datafonoProvider = DatafonoProvider.fromString(config.datafonoProvider),
                            printerIp = config.printerIp,
                            printerModel = PrinterModel.fromString(config.printerModel),
                            readerBrand = ReaderBrand.fromString(config.readerBrand),
                            usePrinterIp = config.usePrinterIp,
                            selectedBluetoothDevice = if (config.printerBluetoothAddress.isNotBlank()) {
                                BluetoothPrinterDevice(
                                    name = config.printerBluetoothName,
                                    address = config.printerBluetoothAddress
                                )
                            } else {
                                null
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Error cargando configuración: ${e.message}")
                }
            }
        }
    }

    private fun checkBluetoothAvailability() {
        _state.update {
            it.copy(
                isBluetoothAvailable = bluetoothPrinterManager.isBluetoothAvailable(),
                isBluetoothEnabled = bluetoothPrinterManager.isBluetoothEnabled(),
                hasBluetoothPermissions = bluetoothPrinterManager.hasBluetoothPermissions()
            )
        }

        // Load devices if everything is ready
        if (_state.value.isBluetoothAvailable &&
            _state.value.isBluetoothEnabled &&
            _state.value.hasBluetoothPermissions
        ) {
            loadBluetoothDevices()
        }
    }

    private fun loadBluetoothDevices() {
        viewModelScope.launch {
            try {
                // Show all paired devices, not just filtered printers
                val devices = bluetoothPrinterManager.getPairedDevices(filterPrinters = false)
                _state.update { it.copy(bluetoothDevices = devices) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Error listando dispositivos Bluetooth: ${e.message}")
                }
            }
        }
    }

    fun onPermissionsResult(granted: Boolean) {
        if (granted) {
            checkBluetoothAvailability()
            loadBluetoothDevices()
        } else {
            _state.update {
                it.copy(error = "Permisos de Bluetooth denegados")
            }
        }
    }

    private fun testPrinter() {
        viewModelScope.launch {
            _state.update { it.copy(isTestingPrinter = true, error = null) }

            try {
                // Validar configuración según el modo
                if (_state.value.usePrinterIp) {
                    // Modo IP
                    if (_state.value.printerIp.isBlank()) {
                        _state.update {
                            it.copy(
                                error = "Debe configurar la IP de la impresora primero",
                                isTestingPrinter = false
                            )
                        }
                        return@launch
                    }
                } else {
                    // Modo Bluetooth
                    val device = _state.value.selectedBluetoothDevice
                    if (device == null) {
                        _state.update {
                            it.copy(
                                error = "Debe seleccionar una impresora Bluetooth primero",
                                isTestingPrinter = false
                            )
                        }
                        return@launch
                    }
                }

                // Text to print
                val testText = "           Megasuper Paraiso            \n       CORPORACION MEGASUPER S.A.       \n            FACTURA ELECTRÓNICA             \n                CONTADO                 \n                                        \nCed:3-101-052164 Tel:                   \nConsecutivo:feltest0001            \nClave:50624011800310719551000100001010000000001123456782                                                  \nFactura:000000038 08/12/2025 12:14:20 a.\nTip. Doc.:*TIQUETE ELECTRÓNICO*         \nCliente:304720192 JOSUE_DE_JESUS REDONDO ARA\nCajero:JOSUE REDONDO ARAYA             i\nReferencia:023078520251207VE000000038  \n--------------------------------------------\nCant.     Descripción   P Unit. M. Total \n  4    ALKA SELTZER X12   2.240     8.960 D \n       DESC     15,08% 1.351,29         \n       PRECIO NETO: 7.609              \n  1    ALKA SELTZER X12   2.240     2.240   \n  1    FRESCO NATURAL 12    500       500   \n  1    TAMARINDO 500g     1.200     1.200  P\n--------------------------------------------\n  7   Art.  Subtotal     :    12.900\n            Descuento    :1351.25   \n                    \n\nCaja: 85    Total a Pagar:    11.549\n Articulos Patrocinadores:         1\n            USTED AHORRO :   135.125\n\n-----------------------------------------  \nTIPO IMP     PRECIO   IMPUESTO      TOTAL\nGR    1%   1,188.12      11.88   1,200.00\nGR    2%   9,655.64     193.11   9,848.75\nGR   13%     442.48      57.52     500.00\n-----------------------------------------\nTotal IVA: 262.52\n\n     =============================      \n* Exento/D Descuento/P Patrocina/E Exonerado\nEstimado Cliente, gracias por tu compra \nhoy, quedás participando de un Cash Back\npara redimir del lunes 06 al viernes 10 \nde enero, podes consultar el monto\nel día de mañana en:                    \n          puntos.megasuper.com          \n         Aplica restricciones.          \n                                        \n  MEGASUPER AHORRO TODOS LOS DIAS!!!!   \n          Servicio al Cliente           \n    Teléfonos:2246-0499/80063-42800     \n                                        \n   Cartago, Paraíso, Llanos de Santa    \n     Lucía, INTERIOR PLAZA PARAISO      \n                                        \n  Impuesto Al Valor Agregado Incluido   \n   Autorizado mediante resolución N°    \nMH-DGT-RES-0027-2024 del 13 de noviembre\n de 2024\n\n                                        \n                                        \n                                        \n                                        \n                                        \n                                        \n                                        \n                                        \n                                        \n                                        \n                                        \n"

                // Print using PrinterManager with custom config
                val result = printerManager.printTextWithConfig(
                    text = testText,
                    printerIp = _state.value.printerIp,
                    bluetoothAddress = _state.value.selectedBluetoothDevice?.address ?: "",
                    usePrinterIp = _state.value.usePrinterIp,
                    printerModel = _state.value.printerModel
                )

                result.fold(
                    onSuccess = { message ->
                        _state.update {
                            it.copy(
                                error = "✓ $message - Revise la impresora",
                                isTestingPrinter = false
                            )
                        }
                    },
                    onFailure = { exception ->
                        _state.update {
                            it.copy(
                                error = "Error al imprimir: ${exception.message}",
                                isTestingPrinter = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Error inesperado: ${e.message}",
                        isTestingPrinter = false
                    )
                }
            }
        }
    }

    private fun saveConfiguration() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Validation
                if (_state.value.hostname.isBlank()) {
                    _state.update {
                        it.copy(
                            error = "El hostname es requerido",
                            isLoading = false
                        )
                    }
                    return@launch
                }

                if (!_state.value.usePrinterIp && _state.value.selectedBluetoothDevice == null) {
                    _state.update {
                        it.copy(
                            error = "Debe seleccionar un dispositivo Bluetooth",
                            isLoading = false
                        )
                    }
                    return@launch
                }

                if (_state.value.usePrinterIp && _state.value.printerIp.isBlank()) {
                    _state.update {
                        it.copy(
                            error = "La IP de la impresora es requerida",
                            isLoading = false
                        )
                    }
                    return@launch
                }

                // Get existing config or create new
                val existingConfig = serverConfigDao.getActiveServerConfigSync()
                val config = ServerConfigEntity(
                    id = existingConfig?.id ?: 1,
                    serverUrl = existingConfig?.serverUrl ?: "",
                    serverName = _state.value.hostname,
                    isActive = true,
                    lastConnected = existingConfig?.lastConnected,
                    datafonUrl = _state.value.datafonUrl,
                    datafonoProvider = _state.value.datafonoProvider.name,
                    printerIp = _state.value.printerIp,
                    printerBluetoothAddress = _state.value.selectedBluetoothDevice?.address ?: "",
                    printerBluetoothName = _state.value.selectedBluetoothDevice?.name ?: "",
                    usePrinterIp = _state.value.usePrinterIp,
                    printerModel = _state.value.printerModel.name,
                    readerBrand = _state.value.readerBrand.name
                )

                serverConfigDao.insertServerConfig(config)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Error guardando configuración: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}
