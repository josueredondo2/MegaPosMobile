package com.devlosoft.megaposmobile.presentation.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.presentation.shared.components.MenuItem
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BillingScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onNavigateToTransaction: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Clear previous customer search results when screen is shown
    LaunchedEffect(Unit) {
        viewModel.onEvent(BillingEvent.ClearCustomerSearch)
    }

    // Handle navigation to transaction screen
    LaunchedEffect(state.shouldNavigateToTransaction) {
        if (state.shouldNavigateToTransaction) {
            viewModel.onEvent(BillingEvent.NavigationHandled)
            onNavigateToTransaction()
        }
    }

    // Error dialog for transaction creation
    state.createTransactionError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissCreateTransactionError) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.DismissCreateTransactionError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Error dialog for recovery check
    state.recoveryCheckError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissRecoveryCheckError) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.DismissRecoveryCheckError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Loading overlay for recovery check
            if (state.isCheckingRecovery) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MegaSuperRed,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Verificando transacciones...",
                            fontSize = dimensions.fontSizeMedium,
                            color = Color.Gray
                        )
                    }
                }
                return@Scaffold
            }

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
                                text = "Volver al menú principal",
                                onClick = onNavigateToHome
                            ),
                            MenuItem(
                                text = "Cerrar sesión",
                                onClick = onLogout
                            )
                        )
                    )
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = dimensions.horizontalPadding)
                ) {
                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Title row with avatar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Nueva Transacción",
                                fontSize = dimensions.fontSizeTitle,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Definir Cliente:",
                                fontSize = dimensions.fontSizeMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Search field
                    OutlinedTextField(
                        value = state.customerSearchQuery,
                        onValueChange = { viewModel.onEvent(BillingEvent.CustomerSearchQueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cliente Identificación") },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.onEvent(BillingEvent.SearchCustomer) },
                                enabled = !state.isSearchingCustomer
                            ) {
                                if (state.isSearchingCustomer) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.onEvent(BillingEvent.SearchCustomer)
                            }
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MegaSuperRed,
                            cursorColor = MegaSuperRed
                        )
                    )

                    // Customer search error message
                    state.customerSearchError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            fontSize = dimensions.fontSizeMedium,
                            color = MegaSuperRed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Customer list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.customers) { customer ->
                            CustomerCard(
                                customer = customer,
                                isSelected = state.selectedCustomer?.partyId == customer.partyId,
                                onClick = { viewModel.onEvent(BillingEvent.SelectCustomer(customer)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Start Transaction button
                    Button(
                        onClick = { viewModel.onEvent(BillingEvent.StartTransaction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.buttonHeight),
                        enabled = !state.isCreatingTransaction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isCreatingTransaction) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (state.transactionCode.isNotBlank()) "Actualizar Cliente" else "Iniciar Transacción",
                                fontSize = dimensions.fontSizeExtraLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val borderColor = if (isSelected) MegaSuperRed else Color.LightGray
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.paddingMedium)
        ) {
            Text(
                text = customer.name,
                fontSize = dimensions.fontSizeLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = customer.identification,
                fontSize = dimensions.fontSizeMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = customer.affiliate,
                fontSize = dimensions.fontSizeMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = customer.identificationDescription,
                fontSize = dimensions.fontSizeMedium,
                color = Color.Gray
            )
        }
    }
}
