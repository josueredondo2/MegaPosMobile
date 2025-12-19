package com.devlosoft.megaposmobile.presentation.billing

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devlosoft.megaposmobile.domain.model.InvoiceItem
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialog
import com.devlosoft.megaposmobile.presentation.shared.components.ConfirmDialog
import com.devlosoft.megaposmobile.presentation.shared.components.ErrorDialog
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.presentation.shared.components.MenuItem
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TransactionScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onNavigateToPayment: (transactionId: String, amount: Double) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
        maximumFractionDigits = 0
    }

    // Transaction menu state
    var showTransactionMenu by remember { mutableStateOf(false) }

    // Selected item state
    var selectedItemId by remember { mutableStateOf<String?>(null) }

    // TODO dialog (from state)
    if (state.showTodoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissTodoDialog) },
            title = { Text("TODO") },
            text = { Text(state.todoDialogMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.DismissTodoDialog) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Error dialog for adding article
    state.addArticleError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissAddArticleError) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.DismissAddArticleError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Error dialog for finalizing transaction
    state.finalizeTransactionError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissFinalizeTransactionError) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.DismissFinalizeTransactionError) }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Authorization Dialog
    AuthorizationDialog(
        state = state.authorizationDialogState,
        onAuthorize = { userCode, password ->
            viewModel.onEvent(BillingEvent.SubmitAuthorization(userCode, password))
        },
        onDismiss = {
            viewModel.onEvent(BillingEvent.DismissAuthorizationDialog)
        },
        onClearError = {
            viewModel.onEvent(BillingEvent.ClearAuthorizationError)
        }
    )

    // Pause Confirmation Dialog
    ConfirmDialog(
        isVisible = state.showPauseConfirmDialog,
        title = "Confirmar Solicitud",
        message = "Seguro que desea pausar la transacción?\n\nPuede ser recuperada en otra estacion de trabajo.",
        confirmText = "Pausar",
        dismissText = "Aun no, volver",
        confirmColor = MegaSuperRed,
        onConfirm = {
            viewModel.onEvent(BillingEvent.ConfirmPauseTransaction)
        },
        onDismiss = {
            viewModel.onEvent(BillingEvent.DismissPauseConfirmDialog)
        }
    )

    // Pause Transaction Error Dialog
    ErrorDialog(
        message = state.pauseTransactionError,
        title = "Error al Pausar",
        onDismiss = {
            viewModel.onEvent(BillingEvent.DismissPauseTransactionError)
        }
    )

    // Print Error Dialog with Retry/Skip options
    if (state.showPrintErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(BillingEvent.DismissPrintErrorDialog) },
            title = {
                Text(
                    text = "Error de Impresión",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = state.printErrorMessage ?: "No se pudo imprimir el comprobante",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(BillingEvent.RetryPrint) },
                    colors = ButtonDefaults.buttonColors(containerColor = MegaSuperRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reintentar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(BillingEvent.SkipPrint) }) {
                    Text("Continuar sin imprimir")
                }
            }
        )
    }

    // Handle navigation after successful pause
    LaunchedEffect(state.shouldNavigateAfterPause) {
        if (state.shouldNavigateAfterPause) {
            viewModel.onEvent(BillingEvent.PauseNavigationHandled)
            onBack()
        }
    }

    Scaffold { paddingValues ->
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

            // Transaction info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.horizontalPadding)
                    .padding(top = dimensions.spacerMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column - Transaction info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Tiquete: ${state.transactionCode.ifBlank { "---" }}",
                        fontSize = dimensions.fontSizeLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cliente: ${state.selectedCustomer?.name ?: "---"}",
                        fontSize = dimensions.fontSizeMedium,
                        color = Color.Black
                    )
                }

                // Right column - Transaction Menu
                Box {
                    IconButton(
                        onClick = { showTransactionMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Menú de transacción",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showTransactionMenu,
                        onDismissRequest = { showTransactionMenu = false }
                    ) {
                        val hasActiveTransaction = state.transactionCode.isNotBlank()
                        val selectedItem = state.invoiceData.items.find { it.itemId == selectedItemId }

                        DropdownMenuItem(
                            text = { Text("Pausar Transacción") },
                            enabled = hasActiveTransaction,
                            onClick = {
                                showTransactionMenu = false
                                viewModel.onEvent(BillingEvent.RequestPauseTransaction)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Abortar Transacción") },
                            enabled = hasActiveTransaction,
                            onClick = {
                                showTransactionMenu = false
                                viewModel.onEvent(BillingEvent.RequestAbortTransaction)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Agregar Envases") },
                            enabled = hasActiveTransaction,
                            onClick = {
                                showTransactionMenu = false
                                viewModel.onEvent(BillingEvent.ShowTodoDialog("Agregar Envases\nItem seleccionado: ${selectedItemId ?: "Ninguno"}"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cambiar Cant. Línea Select.") },
                            enabled = hasActiveTransaction && selectedItemId != null,
                            onClick = {
                                showTransactionMenu = false
                                selectedItem?.let { item ->
                                    viewModel.onEvent(BillingEvent.RequestChangeQuantity(item.itemId, item.itemName))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar Línea Select.") },
                            enabled = hasActiveTransaction && selectedItemId != null,
                            onClick = {
                                showTransactionMenu = false
                                selectedItem?.let { item ->
                                    viewModel.onEvent(BillingEvent.RequestDeleteLine(item.itemId, item.itemName))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mostrar Catálogo") },
                            onClick = {
                                showTransactionMenu = false
                                viewModel.onEvent(BillingEvent.ShowTodoDialog("Mostrar Catálogo\nItem seleccionado: ${selectedItemId ?: "Ninguno"}"))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacerMedium))

            // Article search field
            OutlinedTextField(
                value = state.articleSearchQuery,
                onValueChange = { viewModel.onEvent(BillingEvent.ArticleSearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.horizontalPadding),
                placeholder = { Text("Articulo") },
                enabled = !state.isAddingArticle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        viewModel.onEvent(BillingEvent.AddArticle)
                    }
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MegaSuperRed,
                    cursorColor = MegaSuperRed
                )
            )

            if (state.isAddingArticle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MegaSuperRed,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacerMedium))

            // Items table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Item",
                    fontSize = dimensions.fontSizeMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Cant.",
                    fontSize = dimensions.fontSizeMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Unit.",
                    fontSize = dimensions.fontSizeMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Total",
                    fontSize = dimensions.fontSizeMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = dimensions.horizontalPadding, vertical = 8.dp),
                color = Color.LightGray
            )

            // Items list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensions.horizontalPadding)
            ) {
                items(state.invoiceData.items.filter { !it.isDeleted }) { item ->
                    ItemRow(
                        item = item,
                        numberFormat = numberFormat,
                        isSelected = selectedItemId == item.itemId,
                        onClick = {
                            selectedItemId = if (selectedItemId == item.itemId) null else item.itemId
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Leyenda de iconos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.horizontalPadding)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " Descuento",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " Patrocinador",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.MoneyOff,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " Exento",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Liquor,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " Envase",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Totals section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(horizontal = dimensions.horizontalPadding)
                    .padding(vertical = dimensions.spacerMedium)
            ) {
                TotalRow(
                    label = "Subtotal",
                    value = numberFormat.format(state.invoiceData.totals.subTotal)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TotalRow(
                    label = "Impuestos",
                    value = numberFormat.format(state.invoiceData.totals.tax)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TotalRow(
                    label = "Total Ahorrado",
                    value = numberFormat.format(state.invoiceData.totals.totalSavings)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TotalRow(
                    label = "Total",
                    value = numberFormat.format(state.invoiceData.totals.total),
                    isBold = true
                )
            }

            // Finalize button
            Button(
                onClick = {
                    // Navigate to payment process with transaction ID and total amount
                    onNavigateToPayment(
                        state.transactionCode,
                        state.invoiceData.totals.total
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensions.buttonHeight),
                enabled = state.invoiceData.items.isNotEmpty() && state.transactionCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Text(
                    text = "Finalizar",
                    fontSize = dimensions.fontSizeExtraLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: InvoiceItem,
    numberFormat: NumberFormat,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val dimensions = LocalDimensions.current
    val hasIndicators = item.hasDiscount || item.isSponsor || item.isTaxExempt || item.hasPackaging
    val backgroundColor = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // Fila principal: nombre, cantidad, precio, total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.itemName,
                fontSize = dimensions.fontSizeMedium,
                color = Color.Black,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = item.quantity.toInt().toString(),
                fontSize = dimensions.fontSizeMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = numberFormat.format(item.unitPrice),
                fontSize = dimensions.fontSizeMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = numberFormat.format(item.total),
                fontSize = dimensions.fontSizeMedium,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        if (hasIndicators) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.hasDiscount) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "Descuento",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${item.discountPercentage.toInt()}%",
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (item.isSponsor) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Patrocinador",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (item.isTaxExempt) {
                    Icon(
                        imageVector = Icons.Default.MoneyOff,
                        contentDescription = "Exento",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (item.hasPackaging) {
                    Icon(
                        imageVector = Icons.Default.Liquor,
                        contentDescription = "Envase",
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    val dimensions = LocalDimensions.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = dimensions.fontSizeMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = if (isBold) dimensions.fontSizeLarge else dimensions.fontSizeMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
    }
}
