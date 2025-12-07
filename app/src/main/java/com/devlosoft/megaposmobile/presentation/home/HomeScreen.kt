package com.devlosoft.megaposmobile.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devlosoft.megaposmobile.R
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import com.devlosoft.megaposmobile.ui.theme.MegaSuperWhite

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    // Set logout callback
    LaunchedEffect(Unit) {
        viewModel.setLogoutCallback(onLogout)
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

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Header rojo con logo y botón de usuario
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MegaSuperRed)
                    .height(dimensions.headerHeight)
                    .padding(horizontal = dimensions.paddingMedium),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo image
                    Image(
                        painter = painterResource(id = R.drawable.logo_megasuper),
                        contentDescription = "MegaSuper Logo",
                        modifier = Modifier.height(dimensions.headerHeight * 0.6f),
                        contentScale = ContentScale.FillHeight
                    )
                    // User icon button with dropdown menu
                    Box {
                        IconButton(
                            onClick = { viewModel.onEvent(HomeEvent.ToggleUserMenu) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Usuario",
                                tint = MegaSuperWhite,
                                modifier = Modifier.size(dimensions.iconSizeLarge * 0.6f)
                            )
                        }

                        DropdownMenu(
                            expanded = state.showUserMenu,
                            onDismissRequest = { viewModel.onEvent(HomeEvent.DismissUserMenu) }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cerrar Sesión") },
                                onClick = { viewModel.onEvent(HomeEvent.ShowLogoutConfirmDialog) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Cerrar Sesión"
                                    )
                                }
                            )
                        }
                    }
                }
            }

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
                        text = "Estado: ${state.stationStatus}",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Menu Cards
                    MenuCard(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Aperturar Terminal",
                        description = "Realiza la apertura para el dia.",
                        onClick = { viewModel.onEvent(HomeEvent.OpenTerminal) }
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    MenuCard(
                        icon = Icons.Default.Lock,
                        title = "Cierre Terminal",
                        description = "Realiza el cierre para el dia incluyendo cierre del datafono",
                        onClick = { viewModel.onEvent(HomeEvent.CloseTerminal) }
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    MenuCard(
                        icon = Icons.Default.AttachMoney,
                        title = "Facturación",
                        description = "Ingresa para facturar",
                        onClick = { viewModel.onEvent(HomeEvent.Billing) }
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    MenuCard(
                        icon = Icons.Default.Receipt,
                        title = "Transacciones del dia",
                        description = "Ver transacciones realizadas durante el dia.",
                        onClick = { viewModel.onEvent(HomeEvent.DailyTransactions) }
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))
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
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
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
                    .background(Color(0xFFE8E8E8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF7B68EE),
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
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = dimensions.fontSizeMedium,
                    color = Color.Gray
                )
            }
        }
    }
}
