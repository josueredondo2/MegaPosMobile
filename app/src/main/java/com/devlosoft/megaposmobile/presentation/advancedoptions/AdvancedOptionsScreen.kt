package com.devlosoft.megaposmobile.presentation.advancedoptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devlosoft.megaposmobile.core.util.BluetoothPrinterDevice
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import com.devlosoft.megaposmobile.ui.theme.MegaSuperWhite

@Composable
fun AdvancedOptionsScreen(
    viewModel: AdvancedOptionsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val dimensions = LocalDimensions.current

    val permissionState = rememberBluetoothPermissionState(
        onPermissionsResult = { granted ->
            viewModel.onEvent(AdvancedOptionsEvent.OnPermissionsResult(granted))
        }
    )

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("Configuración guardada exitosamente")
            viewModel.onEvent(AdvancedOptionsEvent.ClearSavedFlag)
            onBack()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(AdvancedOptionsEvent.ClearError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Header
            AppHeader(
                endContent = HeaderEndContent.VersionText(version = "1.0")
            )

            // Contenido centrado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = dimensions.maxContentWidth)
                        .padding(horizontal = dimensions.horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(dimensions.spacerExtraLarge))

                    // Título
                    Text(
                        text = "Opciones Avanzadas",
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerSmall))

                    // Subtítulo
                    Text(
                        text = "Configurar dispositivos y servicios",
                        fontSize = dimensions.fontSizeLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Hostname Field
                    OutlinedTextField(
                        value = state.hostname,
                        onValueChange = { viewModel.onEvent(AdvancedOptionsEvent.HostnameChanged(it)) },
                        label = {
                            Text(
                                text = "Host Name",
                                fontSize = dimensions.fontSizeMedium
                            )
                        },
                        placeholder = {
                            Text(
                                text = "android-pos-01",
                                fontSize = dimensions.fontSizeMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.textFieldHeight),
                        enabled = !state.isLoading,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = dimensions.fontSizeMedium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MegaSuperRed,
                            focusedLabelColor = MegaSuperRed,
                            cursorColor = MegaSuperRed
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Datafono URL Field
                    OutlinedTextField(
                        value = state.datafonUrl,
                        onValueChange = { viewModel.onEvent(AdvancedOptionsEvent.DatafonUrlChanged(it)) },
                        label = {
                            Text(
                                text = "Dirección URL Datafono",
                                fontSize = dimensions.fontSizeMedium
                            )
                        },
                        placeholder = {
                            Text(
                                text = "http://192.168.1.100:8080",
                                fontSize = dimensions.fontSizeMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.textFieldHeight),
                        enabled = !state.isLoading,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = dimensions.fontSizeMedium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MegaSuperRed,
                            focusedLabelColor = MegaSuperRed,
                            cursorColor = MegaSuperRed
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Printer Section Title
                    Text(
                        text = "Configuración de Impresora",
                        fontSize = dimensions.fontSizeLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Printer Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Usar impresora por IP",
                            fontSize = dimensions.fontSizeMedium,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.usePrinterIp,
                            onCheckedChange = {
                                viewModel.onEvent(AdvancedOptionsEvent.PrinterModeChanged(it))
                            },
                            enabled = !state.isLoading,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MegaSuperRed,
                                checkedTrackColor = MegaSuperRed.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Conditional: IP or Bluetooth
                    if (state.usePrinterIp) {
                        // Printer IP Field
                        OutlinedTextField(
                            value = state.printerIp,
                            onValueChange = { viewModel.onEvent(AdvancedOptionsEvent.PrinterIpChanged(it)) },
                            label = {
                                Text(
                                    text = "IP Impresora",
                                    fontSize = dimensions.fontSizeMedium
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "192.168.1.200",
                                    fontSize = dimensions.fontSizeMedium
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensions.textFieldHeight),
                            enabled = !state.isLoading,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = dimensions.fontSizeMedium
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MegaSuperRed,
                                focusedLabelColor = MegaSuperRed,
                                cursorColor = MegaSuperRed
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.onEvent(AdvancedOptionsEvent.Save)
                                }
                            )
                        )
                    } else {
                        // Bluetooth Printer Selector
                        BluetoothPrinterSelector(
                            state = state,
                            permissionState = permissionState,
                            onDeviceSelected = { device ->
                                viewModel.onEvent(AdvancedOptionsEvent.BluetoothDeviceSelected(device))
                            },
                            onRefresh = {
                                viewModel.onEvent(AdvancedOptionsEvent.RefreshBluetoothDevices)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Test Printer Button
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.onEvent(AdvancedOptionsEvent.TestPrinter) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.buttonHeight),
                        enabled = !state.isLoading && !state.isTestingPrinter,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MegaSuperRed
                        )
                    ) {
                        if (state.isTestingPrinter) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensions.iconSizeMedium),
                                color = MegaSuperRed,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Probar Impresión",
                                fontSize = dimensions.fontSizeExtraLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Save Button
                    Button(
                        onClick = { viewModel.onEvent(AdvancedOptionsEvent.Save) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.buttonHeight),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            contentColor = MegaSuperWhite
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensions.iconSizeMedium),
                                color = MegaSuperWhite
                            )
                        } else {
                            Text(
                                text = "Guardar",
                                fontSize = dimensions.fontSizeExtraLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothPrinterSelector(
    state: AdvancedOptionsState,
    permissionState: BluetoothPermissionState,
    onDeviceSelected: (BluetoothPrinterDevice) -> Unit,
    onRefresh: () -> Unit
) {
    val dimensions = LocalDimensions.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            !state.isBluetoothAvailable -> {
                Text(
                    text = "Bluetooth no disponible en este dispositivo",
                    fontSize = dimensions.fontSizeMedium,
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            !permissionState.hasPermissions -> {
                Column {
                    Text(
                        text = "Se requieren permisos de Bluetooth",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { permissionState.requestPermissions() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed
                        )
                    ) {
                        Text("Conceder Permisos")
                    }
                }
            }
            !state.isBluetoothEnabled -> {
                Text(
                    text = "Por favor active Bluetooth en la configuración del dispositivo",
                    fontSize = dimensions.fontSizeMedium,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                // Dropdown with devices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                        ) {
                            OutlinedTextField(
                                value = state.selectedBluetoothDevice?.name ?: "Seleccionar impresora Bluetooth",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MegaSuperRed,
                                    cursorColor = MegaSuperRed,
                                    disabledBorderColor = Color.Gray,
                                    disabledTextColor = Color.Black
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = dimensions.fontSizeMedium
                                )
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.bluetoothDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay dispositivos pareados") },
                                    onClick = { }
                                )
                            } else {
                                state.bluetoothDevices.forEach { device ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = device.name,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = device.address,
                                                    fontSize = dimensions.fontSizeSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            onDeviceSelected(device)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MegaSuperRed
                        )
                    }
                }
            }
        }
    }
}
