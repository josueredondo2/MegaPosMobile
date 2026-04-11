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
            is AdvancedOptionsEvent.InactivityTimeoutChanged -> {
                // Only allow digits and validate range (1-60)
                val filtered = event.minutes.filter { it.isDigit() }
                _state.update { it.copy(inactivityTimeoutMinutes = filtered) }
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
            is AdvancedOptionsEvent.ConfigureHabladores -> {
                configurePrinter(isHabladores = true)
            }
            is AdvancedOptionsEvent.ConfigureMegaPos -> {
                configurePrinter(isHabladores = false)
            }
            is AdvancedOptionsEvent.TestCpcl1x2 -> {
                testCpcl("1x2")
            }
            is AdvancedOptionsEvent.TestCpcl2x3 -> {
                testCpcl("2x3")
            }
            is AdvancedOptionsEvent.TestCpcl7x10 -> {
                testCpcl("7x10")
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
                            inactivityTimeoutMinutes = config.inactivityTimeoutMinutes.toString(),
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

    private fun testCpcl(size: String) {
        viewModelScope.launch {
            _state.update { it.copy(isTestingPrinter = true, error = null) }

            try {
                if (_state.value.usePrinterIp && _state.value.printerIp.isBlank()) {
                    _state.update { it.copy(error = "Debe configurar la IP primero", isTestingPrinter = false) }
                    return@launch
                }
                if (!_state.value.usePrinterIp && _state.value.selectedBluetoothDevice == null) {
                    _state.update { it.copy(error = "Debe seleccionar impresora Bluetooth", isTestingPrinter = false) }
                    return@launch
                }

                val cpclTest = when (size) {
                    "1x2" -> buildString {
                        // Hablador Estándar 1x2 RP4 (48x27mm) - hae_codigo=40, tone=68
                        appendLine("! 0 200 200 27 1")
                        appendLine("IN-MILLIMETERS")
                        appendLine("PW 48")
                        appendLine("PREFEED -1")
                        appendLine("POSTFEED 0")
                        appendLine("LABEL")
                        appendLine("TONE 68")
                        appendLine("COUNTRY CP850")
                        // art_nombre_completo (alin=1 CENTER, font=0, size=0, x=0, y=0.6)
                        appendLine("CENTER 48")
                        appendLine("T 0 0 0 0.6 FRESCO NATURAL 12 OZ")
                        // descuento (alin=2 RIGHT, font=7, size=0, x=1, y=6.5, inverso=1)
                        appendLine("RIGHT 48")
                        appendLine("INVERSE")
                        appendLine("T 7 0 1 6.5 OFERTA")
                        // precio (alin=0 LEFT, font=4, size=3, x=4, y=7.8)
                        appendLine("LEFT 48")
                        appendLine("T 4 3 4 7.8 570")
                        // | (alin=0, font=4, size=0, x=2, y=9.3)
                        appendLine("LEFT 48")
                        appendLine("T 4 0 2 9.3 |")
                        // C (alin=0, font=4, size=0, x=1, y=9.8)
                        appendLine("LEFT 48")
                        appendLine("T 4 0 1 9.8 C")
                        // fecha (alin=1 CENTER, font=0, size=0, x=1, y=17.7)
                        appendLine("CENTER 48")
                        appendLine("T 0 0 1 17.7 06/04/2026")
                        // top600 (alin=0 LEFT, font=4, size=3, x=41, y=18)
                        appendLine("LEFT 48")
                        appendLine("T 4 3 41 18 *")
                        // barcode (alin=1 CENTER, UCCEAN128, w=0.2, ratio=3, h=3, x=1, y=19)
                        appendLine("CENTER 48")
                        appendLine("BARCODE UCCEAN128 0.2 3 3 1 19 5051")
                        // codigo texto (alin=1 CENTER, font=7, size=0, x=1, y=22.1)
                        appendLine("CENTER 48")
                        appendLine("T 7 0 1 22.1 5051")
                        // C (alin=0, font=0, size=3, x=1, y=24.4)
                        appendLine("LEFT 48")
                        appendLine("T 0 3 1 24.4 C")
                        // | (alin=0, font=0, size=3, x=2, y=24.4)
                        appendLine("LEFT 48")
                        appendLine("T 0 3 2 24.4 |")
                        // unidad (alin=0, font=0, size=3, x=3, y=24.4)
                        appendLine("LEFT 48")
                        appendLine("T 0 3 3 24.4 UND")
                        // ubicacion (alin=0, font=0, size=0, x=37, y=26.1)
                        appendLine("LEFT 48")
                        appendLine("T 0 0 37 26.1 A01-P03")
                        appendLine("CUT")
                        appendLine("FORM")
                        appendLine("PRINT")
                    }
                    "2x3" -> buildString {
                        // Hab.Des.Ahorre 2x3 (77x50mm) - hae_codigo=26, tone=0
                        appendLine("! 0 200 200 50 1")
                        appendLine("IN-MILLIMETERS")
                        appendLine("PW 77")
                        appendLine("PREFEED 0")
                        appendLine("POSTFEED 0")
                        appendLine("LABEL")
                        appendLine("TONE 0")
                        appendLine("COUNTRY CP850")
                        appendLine("LEFT 77")
                        appendLine("T 5 1 6 3 FRESCO NATURAL 12 OZ")
                        appendLine("LEFT 77")
                        appendLine("T 5 3 60 5 *")
                        appendLine("LEFT 77")
                        appendLine("T 5 1 6 8 BEBIDAS")
                        appendLine("LEFT 77")
                        appendLine("T 4 4 19.5 11.5 570")
                        appendLine("LEFT 77")
                        appendLine("T 4 1 14 17 C")
                        appendLine("LEFT 77")
                        appendLine("T 4 1 15 16.5 |")
                        appendLine("LEFT 77")
                        appendLine("T 0 2 0 31 COSTA RICA")
                        appendLine("LEFT 77")
                        appendLine("T 0 2 40 31 APLICA SOLO CON CLIENTE PLATA")
                        appendLine("LEFT 77")
                        appendLine("T 5 0 21 34.5 PRECIO REGULAR:")
                        appendLine("LEFT 77")
                        appendLine("T 5 0 55 34.5 AHORRE:")
                        appendLine("LEFT 77")
                        appendLine("T 5 1 23 37 570")
                        appendLine("LEFT 77")
                        appendLine("T 5 1 57 37 100")
                        appendLine("LEFT 77")
                        appendLine("T 5 0 20 38.5 C")
                        appendLine("LEFT 77")
                        appendLine("T 5 0 21 38.5 |")
                        appendLine("LEFT 77")
                        appendLine("T 5 0 54 38.5 C")
                        appendLine("LEFT 77")
                        appendLine("T 5 0 55 38.5 |")
                        appendLine("LEFT 77")
                        appendLine("T 0 2 52 43 UND")
                        appendLine("LEFT 77")
                        appendLine("T 0 2 28 43 LUN-DOM")
                        appendLine("LEFT 77")
                        appendLine("T 0 2 30 46 06/04-30/04")
                        appendLine("FORM")
                        appendLine("PRINT")
                    }
                    "7x10" -> buildString {
                        // Hab.Descuento 7x10 (102x102mm) - hae_codigo=21, tone=123
                        // TODOS los campos tienen hde_rotacion=1 → T90 y VBARCODE
                        appendLine("! 0 200 200 102 1")
                        appendLine("IN-MILLIMETERS")
                        appendLine("PW 102")
                        appendLine("PREFEED -1")
                        appendLine("POSTFEED 1")
                        appendLine("LABEL")
                        appendLine("TONE 123")
                        appendLine("COUNTRY CP850")
                        // AHORRO: (font=7, size=1, x=53, y=23)
                        appendLine("LEFT 102")
                        appendLine("T90 7 1 53 23 AHORRO:")
                        // ahorro valor (font=4, size=0, x=58, y=23)
                        appendLine("LEFT 102")
                        appendLine("T90 4 0 58 23  100")
                        // | (font=4, size=0, x=58, y=23)
                        appendLine("LEFT 102")
                        appendLine("T90 4 0 58 23 |")
                        // C (font=4, size=0, x=58, y=24)
                        appendLine("LEFT 102")
                        appendLine("T90 4 0 58 24 C")
                        // PaisOrigen (font=7, size=1, x=43, y=56)
                        appendLine("LEFT 102")
                        appendLine("T90 7 1 43 56 COSTA RICA")
                        // APLICA SOLO CON TARJETA PLATA (font=7, size=1, x=48, y=56)
                        appendLine("LEFT 102")
                        appendLine("T90 7 1 48 56 APLICA SOLO CON TARJETA PLATA")
                        // dias (alin=1 CENTER, font=7, size=0, x=67, y=60)
                        appendLine("CENTER 102")
                        appendLine("T90 7 0 67 60 LUN-DOM")
                        // precio grande (font=4, size=6, x=7, y=69)
                        appendLine("LEFT 102")
                        appendLine("T90 4 6 7 69 570")
                        // | (font=4, size=1, x=24, y=76)
                        appendLine("LEFT 102")
                        appendLine("T90 4 1 24 76 |")
                        // C (font=4, size=1, x=24, y=77)
                        appendLine("LEFT 102")
                        appendLine("T90 4 1 24 77 C")
                        // vigencia (font=7, size=0, x=64, y=80)
                        appendLine("LEFT 102")
                        appendLine("T90 7 0 64 80 06/04-30/04")
                        // unidad (font=2, size=1, x=6, y=95)
                        appendLine("LEFT 102")
                        appendLine("T90 2 1 6 95 UND")
                        // barcode (128, w=0.2, ratio=0, h=3.2, x=47, y=96) - rotado vertical
                        appendLine("LEFT 102")
                        appendLine("VBARCODE 128 0.2 0 3.2 47 96 5051")
                        // codigo texto (font=0, size=2, x=51, y=96)
                        appendLine("LEFT 102")
                        appendLine("T90 0 2 51 96 5051")
                        // PRECIO REGULAR: (font=7, size=1, x=53, y=96)
                        appendLine("LEFT 102")
                        appendLine("T90 7 1 53 96 PRECIO REGULAR:")
                        // precio regular (font=4, size=0, x=58, y=96)
                        appendLine("LEFT 102")
                        appendLine("T90 4 0 58 96  570")
                        // | (font=4, size=0, x=58, y=96)
                        appendLine("LEFT 102")
                        appendLine("T90 4 0 58 96 |")
                        // C (font=4, size=0, x=58, y=97)
                        appendLine("LEFT 102")
                        appendLine("T90 4 0 58 97 C")
                        // C (font=2, size=1, x=6, y=98)
                        appendLine("LEFT 102")
                        appendLine("T90 2 1 6 98 C")
                        // | (font=2, size=1, x=6, y=98)
                        appendLine("LEFT 102")
                        appendLine("T90 2 1 6 98 |")
                        // art_nombre (font=2, size=1, x=2, y=99)
                        appendLine("LEFT 102")
                        appendLine("T90 2 1 2 99 FRESCO NATURAL 12 OZ")
                        appendLine("FORM")
                        appendLine("PRINT")
                    }
                    else -> return@launch
                }

                val result = printerManager.sendRawCommandWithConfig(
                    command = cpclTest,
                    printerIp = _state.value.printerIp,
                    bluetoothAddress = _state.value.selectedBluetoothDevice?.address ?: "",
                    usePrinterIp = _state.value.usePrinterIp
                )

                result.fold(
                    onSuccess = {
                        _state.update { it.copy(error = "Test CPCL $size enviado - Revise la impresora", isTestingPrinter = false) }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = "Error: ${e.message}", isTestingPrinter = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error: ${e.message}", isTestingPrinter = false) }
            }
        }
    }

    private fun configurePrinter(isHabladores: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isConfiguringPrinter = true, error = null) }

            try {
                if (_state.value.usePrinterIp) {
                    if (_state.value.printerIp.isBlank()) {
                        _state.update {
                            it.copy(
                                error = "Debe configurar la IP de la impresora primero",
                                isConfiguringPrinter = false
                            )
                        }
                        return@launch
                    }
                } else {
                    if (_state.value.selectedBluetoothDevice == null) {
                        _state.update {
                            it.copy(
                                error = "Debe seleccionar una impresora Bluetooth primero",
                                isConfiguringPrinter = false
                            )
                        }
                        return@launch
                    }
                }

                val commands = if (isHabladores) {
                    buildString {
                        appendLine("! U1 setvar \"device.languages\" \"cpcl\"")
                        appendLine("! U1 setvar \"device.pnp_option\" \"cpcl\"")
                        appendLine("! U1 setvar \"media.type\" \"label\"")
                        appendLine("! U1 setvar \"media.sense_mode\" \"bar\"")
                        appendLine("! U1 setvar \"media.printmode\" \"tear off\"")
                        appendLine("! U1 setvar \"ezpl.power_up_action\" \"calibrate\"")
                        appendLine("! U1 setvar \"ezpl.head_close_action\" \"calibrate\"")
                        appendLine("! U1 setvar \"device.reset\" \"\"")
                    }
                } else {
                    buildString {
                        appendLine("! U1 setvar \"device.languages\" \"zpl\"")
                        appendLine("! U1 setvar \"device.pnp_option\" \"zpl\"")
                        appendLine("! U1 setvar \"media.type\" \"journal\"")
                        appendLine("! U1 setvar \"media.printmode\" \"tear off\"")
                        appendLine("! U1 setvar \"ezpl.power_up_action\" \"no motion\"")
                        appendLine("! U1 setvar \"ezpl.head_close_action\" \"no motion\"")
                        appendLine("! U1 setvar \"device.reset\" \"\"")
                    }
                }

                val configName = if (isHabladores) "Habladores (CPCL)" else "MegaPos (ZPL)"

                val result = printerManager.sendRawCommandWithConfig(
                    command = commands,
                    printerIp = _state.value.printerIp,
                    bluetoothAddress = _state.value.selectedBluetoothDevice?.address ?: "",
                    usePrinterIp = _state.value.usePrinterIp
                )

                result.fold(
                    onSuccess = {
                        _state.update {
                            it.copy(
                                error = "Impresora configurada para $configName. Se reiniciará.",
                                isConfiguringPrinter = false,
                                printerLanguage = if (isHabladores) "cpcl" else "zpl"
                            )
                        }
                    },
                    onFailure = { exception ->
                        _state.update {
                            it.copy(
                                error = "Error al configurar: ${exception.message}",
                                isConfiguringPrinter = false
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Error inesperado: ${e.message}",
                        isConfiguringPrinter = false
                    )
                }
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
                val testText = "Impresion Exitosa\n\n\n\n\n"

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

                // Validate inactivity timeout (1-60 minutes)
                val timeoutMinutes = _state.value.inactivityTimeoutMinutes.toIntOrNull() ?: 0
                if (timeoutMinutes < 1 || timeoutMinutes > 60) {
                    _state.update {
                        it.copy(
                            error = "El tiempo de inactividad debe ser entre 1 y 60 minutos",
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
                    readerBrand = _state.value.readerBrand.name,
                    inactivityTimeoutMinutes = timeoutMinutes
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
