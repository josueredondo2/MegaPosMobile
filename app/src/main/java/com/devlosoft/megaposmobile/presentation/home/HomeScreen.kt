package com.devlosoft.megaposmobile.presentation.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialog
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.presentation.shared.components.MenuItem
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigateToProcess: (String) -> Unit = {},
    onNavigateToBilling: () -> Unit = {},
    onNavigateToAdvancedOptions: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    // Set callbacks
    LaunchedEffect(Unit) {
        viewModel.setLogoutCallback(onLogout)
        viewModel.setNavigateToBillingCallback(onNavigateToBilling)
        viewModel.setNavigateToProcessCallback(onNavigateToProcess)
        viewModel.setNavigateToAdvancedOptionsCallback(onNavigateToAdvancedOptions)
    }

    // Todo Dialog
    if (state.showTodoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissDialog) },
            title = { Text(text = state.todoDialogTitle) },
            text = { Text(text = "TODO: Implementar ${state.todoDialogTitle}") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissDialog) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (state.showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissLogoutConfirmDialog) },
            title = { Text(text = "Cerrar Sesión") },
            text = { Text(text = "¿Está seguro que desea cerrar la sesión?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.ConfirmLogout) }) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissLogoutConfirmDialog) }) {
                    Text("No")
                }
            }
        )
    }

    // Printer Error Dialog
    state.printerError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissPrinterError) },
            title = { Text(text = "Error de Impresora") },
            text = { Text(text = errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissPrinterError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Authorization Dialog
    AuthorizationDialog(
        state = state.authorizationDialogState,
        onAuthorize = { userCode, password ->
            viewModel.onEvent(HomeEvent.SubmitAuthorization(userCode, password))
        },
        onDismiss = {
            viewModel.onEvent(HomeEvent.DismissAuthorizationDialog)
        },
        onClearError = {
            viewModel.onEvent(HomeEvent.ClearAuthorizationError)
        }
    )

    // Close Datafono Confirmation Dialog
    if (state.showCloseDatafonoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissCloseDatafonoConfirmDialog) },
            title = { Text(text = "Cierre de Datafono") },
            text = { Text(text = "¿Está seguro que desea realizar el cierre del lote de ventas del datafono?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.ConfirmCloseDatafono) }) {
                    Text("Sí, Cerrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissCloseDatafonoConfirmDialog) }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Close Datafono Loading Dialog
    if (state.isClosingDatafono) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss while loading */ },
            title = { Text(text = "Procesando Cierre") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Comunicando con el datafono...")
                }
            },
            confirmButton = { }
        )
    }

    // Close Datafono Error Dialog
    state.closeDatafonoError?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissCloseDatafonoError) },
            title = { Text(text = "Error en Cierre") },
            text = { Text(text = errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissCloseDatafonoError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Close Datafono Success Dialog
    if (state.showCloseDatafonoSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissCloseDatafonoSuccess) },
            title = { Text(text = "Cierre Exitoso") },
            text = {
                Column {
                    Text(text = state.closeDatafonoMessage)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ventas: ${state.closeDatafonoSalesCount}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ₡${String.format("%,.2f", state.closeDatafonoSalesTotal)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(HomeEvent.DismissCloseDatafonoSuccess) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
            // Header
            AppHeader(
                endContent = HeaderEndContent.UserMenu(
                    items = listOf(
                        MenuItem(
                            text = "Cerrar Sesión",
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            onClick = { viewModel.onEvent(HomeEvent.ShowLogoutConfirmDialog) }
                        )
                    ),
                    expanded = state.showUserMenu,
                    onExpandedChange = { isExpanded ->
                        if (isExpanded) {
                            viewModel.onEvent(HomeEvent.ToggleUserMenu)
                        } else {
                            viewModel.onEvent(HomeEvent.DismissUserMenu)
                        }
                    }
                )
            )

            // Contenido
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = dimensions.maxContentWidth)
                        .padding(horizontal = dimensions.horizontalPadding)
                ) {
                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Sección de bienvenida
                    Text(
                        text = "Bienvenido: ${state.userName}",
                        fontSize = dimensions.fontSizeMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerSmall))

                    Text(
                        text = "Fecha: ${state.currentDate}",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Terminal: ${state.terminalName}",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Estado de Terminal: ${state.stationStatus}",
                        fontSize = dimensions.fontSizeMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isStationOpen) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Menu Cards - shown based on user permissions (show property)
                    // onClick uses Request* events to validate access before executing
                    if (state.canOpenTerminal) {
                        MenuCard(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Aperturar Terminal",
                            description = "Realiza la apertura para el dia.",
                            onClick = { viewModel.onEvent(HomeEvent.RequestOpenTerminal) }
                        )
                        Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                    }

                    if (state.canCloseTerminal) {
                        MenuCard(
                            icon = Icons.Default.Lock,
                            title = "Cierre Terminal",
                            description = "Realiza el cierre para el dia incluyendo cierre del datafono",
                            onClick = { viewModel.onEvent(HomeEvent.RequestCloseTerminal) }
                        )
                        Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                    }

                    if (state.canCloseDatafono) {
                        MenuCard(
                            icon = Icons.Default.Receipt,
                            title = "Cierre de datafono",
                            description = "Cerrar el lote de ventas del datafono.",
                            onClick = { viewModel.onEvent(HomeEvent.RequestCloseDatafono) }
                        )
                        Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                    }

                    if (state.canBilling) {
                        MenuCard(
                            icon = Icons.Default.AttachMoney,
                            title = "Facturación",
                            description = "Ingresa para facturar",
                            enabled = state.isStationOpen && !state.isCheckingPrinter,
                            onClick = { viewModel.onEvent(HomeEvent.RequestBilling) }
                        )
                        Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                    }

                    if (state.canViewTransactions) {
                        MenuCard(
                            icon = Icons.Default.Receipt,
                            title = "Transacciones del dia",
                            description = "Ver transacciones realizadas durante el dia.",
                            onClick = { viewModel.onEvent(HomeEvent.RequestViewTransactions) }
                        )
                        Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                    }

                    if (state.canAdvancedOptions) {
                        MenuCard(
                            icon = Icons.Default.Settings,
                            title = "Opciones Avanzadas",
                            description = "Configurar impresora, datafono y otros dispositivos",
                            onClick = { viewModel.onEvent(HomeEvent.RequestAdvancedOptions) }
                        )
                        Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))
                }
            }
            }
        }
    }
}

@Composable
fun MenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val alpha = if (enabled) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color.White else Color(0xFFF0F0F0)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(dimensions.iconSizeLarge * 0.8f)
                    .clip(CircleShape)
                    .background(if (enabled) Color(0xFFE8E8E8) else Color(0xFFD8D8D8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) Color(0xFF7B68EE) else Color(0xFFAAAAAA),
                    modifier = Modifier.size(dimensions.iconSizeMedium)
                )
            }

            Spacer(modifier = Modifier.width(dimensions.spacerMedium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = dimensions.fontSizeLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.Black else Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = dimensions.fontSizeMedium,
                    color = if (enabled) Color.Gray else Color.LightGray
                )
            }
        }
    }
}
