package com.devlosoft.megaposmobile.presentation.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivityDto
import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivitySearchItemDto
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.presentation.shared.components.MenuItem
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BillingScreen(
    resetState: Boolean = false,
    viewModel: BillingViewModel = hiltViewModel(),
    onNavigateToTransaction: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Clear previous customer search results or reset entire state based on resetState flag

    LaunchedEffect(resetState) {
        if (resetState) {
            // Reset entire state when coming from completed transaction
            viewModel.onEvent(BillingEvent.ResetForNewTransaction)
        }
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

    // Error dialog for FEL client validation
    state.clientValidationError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissClientValidationError) },
            title = { Text("Error de Validación") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.DismissClientValidationError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Economic Activity Selection Dialog
    if (state.showActivityDialog) {
        EconomicActivityDialog(
            activities = state.economicActivities,
            searchedActivities = state.searchedActivities,
            searchQuery = state.activitySearchQuery,
            selectedActivity = state.selectedActivity,
            selectedSearchActivity = state.selectedSearchActivity,
            isSearching = state.isSearchingActivities,
            isLoadingMore = state.isLoadingMoreActivities,
            hasNextPage = state.activityHasNextPage,
            onSearchQueryChanged = { viewModel.onEvent(BillingEvent.ActivitySearchQueryChanged(it)) },
            onSearch = { viewModel.onEvent(BillingEvent.SearchActivities) },
            onLoadMore = { viewModel.onEvent(BillingEvent.LoadMoreActivities) },
            onActivitySelected = { viewModel.onEvent(BillingEvent.SelectActivity(it)) },
            onSearchActivitySelected = { viewModel.onEvent(BillingEvent.SelectSearchActivity(it)) },
            onConfirm = { viewModel.onEvent(BillingEvent.ConfirmActivitySelection) },
            onDismiss = { viewModel.onEvent(BillingEvent.DismissActivityDialog) }
        )
    }

    @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { _ ->
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
                    .background(Color.White)
                    .windowInsetsPadding(WindowInsets.navigationBars)
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

                    // Title
                    Text(
                        text = "Nueva Transacción",
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerSmall))

                    // Tipo Documento label
                    Text(
                        text = "Tipo Documento:",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tabs de tipo de documento (estilo pill)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tab Tiquete Electronico
                        Button(
                            onClick = { viewModel.onEvent(BillingEvent.DocumentTypeChanged("CO")) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.documentType == "CO") MegaSuperRed else Color.White,
                                contentColor = if (state.documentType == "CO") Color.White else Color.Gray
                            ),
                            border = BorderStroke(1.dp, if (state.documentType == "CO") MegaSuperRed else Color.LightGray),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Tiquete Electronico",
                                fontSize = dimensions.fontSizeMedium
                            )
                        }

                        // Tab Factura Electronica Contado
                        Button(
                            onClick = { viewModel.onEvent(BillingEvent.DocumentTypeChanged("FC")) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.documentType == "FC") MegaSuperRed else Color.White,
                                contentColor = if (state.documentType == "FC") Color.White else Color.Gray
                            ),
                            border = BorderStroke(1.dp, if (state.documentType == "FC") MegaSuperRed else Color.LightGray),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Factura Electronica",
                                fontSize = dimensions.fontSizeMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Definir Cliente label
                    Text(
                        text = "Definir Cliente:",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                        enabled = !state.isCreatingTransaction && !state.isValidatingClient,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isCreatingTransaction || state.isValidatingClient) {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EconomicActivityDialog(
    activities: List<EconomicActivityDto>,
    searchedActivities: List<EconomicActivitySearchItemDto>,
    searchQuery: String,
    selectedActivity: EconomicActivityDto?,
    selectedSearchActivity: EconomicActivitySearchItemDto?,
    isSearching: Boolean,
    isLoadingMore: Boolean,
    hasNextPage: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onActivitySelected: (EconomicActivityDto) -> Unit,
    onSearchActivitySelected: (EconomicActivitySearchItemDto) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Determine if we have a valid selection
    val hasSelection = selectedActivity != null || selectedSearchActivity != null

    // Show searched activities if we have them, otherwise show the original activities from validation
    val showSearchResults = searchedActivities.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buscador Actividades\nEconomicas",
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Search field - triggers search on Enter/OK
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción") },
                    placeholder = { Text("Indique Actividad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            onSearch()
                        }
                    ),
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MegaSuperRed
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MegaSuperRed,
                        cursorColor = MegaSuperRed
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Activity list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showSearchResults) {
                        // Show search results
                        items(searchedActivities) { activity ->
                            SearchActivityCard(
                                activity = activity,
                                isSelected = selectedSearchActivity?.code == activity.code,
                                onClick = { onSearchActivitySelected(activity) }
                            )
                        }

                        // Load more button/indicator
                        if (hasNextPage) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = MegaSuperRed
                                        )
                                    } else {
                                        TextButton(onClick = onLoadMore) {
                                            Text("Cargar más...", color = MegaSuperRed)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Show original activities from validation (sorted by type P first)
                        items(activities) { activity ->
                            ActivityCard(
                                activity = activity,
                                isSelected = selectedActivity?.code == activity.code,
                                onClick = { onActivitySelected(activity) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Salir", color = Color.Black)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = hasSelection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Seleccionar")
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: EconomicActivityDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val borderColor = if (isSelected) MegaSuperRed else Color.LightGray
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = if (isSelected) MegaSuperRed.copy(alpha = 0.1f) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Text(
            text = activity.description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            fontSize = dimensions.fontSizeMedium,
            color = Color.Black
        )
    }
}

@Composable
fun SearchActivityCard(
    activity: EconomicActivitySearchItemDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current
    val borderColor = if (isSelected) MegaSuperRed else Color.LightGray
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = if (isSelected) MegaSuperRed.copy(alpha = 0.1f) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Text(
            text = activity.description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            fontSize = dimensions.fontSizeMedium,
            color = Color.Black
        )
    }
}
