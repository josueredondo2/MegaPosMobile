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
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.devlosoft.megaposmobile.core.scanner.BarcodeScannerHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.domain.model.InvoiceItem
import com.devlosoft.megaposmobile.presentation.shared.components.AbortConfirmDialog
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialog
import com.devlosoft.megaposmobile.presentation.shared.components.ChangeQuantityDialog
import com.devlosoft.megaposmobile.presentation.shared.components.ConfirmDialog
import com.devlosoft.megaposmobile.presentation.shared.components.ErrorDialog
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.presentation.shared.components.MenuItem
import com.devlosoft.megaposmobile.presentation.billing.components.PackagingDialog
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

    // Barcode scanner handler for Zebra/PAX hardware scanners
    val scannerHandler = remember { BarcodeScannerHandler() }

    // Track if the article TextField has focus
    var isArticleFieldFocused by remember { mutableStateOf(false) }

    // Clean up scanner buffer when leaving screen
    DisposableEffect(Unit) {
        onDispose { scannerHandler.reset() }
    }

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

    // Abort Confirmation Dialog with reason text field
    AbortConfirmDialog(
        isVisible = state.showAbortConfirmDialog,
        reason = state.abortReason,
        onReasonChanged = { viewModel.onEvent(BillingEvent.AbortReasonChanged(it)) },
        onConfirm = { viewModel.onEvent(BillingEvent.ConfirmAbortTransaction) },
        onDismiss = { viewModel.onEvent(BillingEvent.DismissAbortConfirmDialog) }
    )

    // Abort Transaction Error Dialog
    ErrorDialog(
        message = state.abortTransactionError,
        title = "Error al Abortar",
        onDismiss = {
            viewModel.onEvent(BillingEvent.DismissAbortTransactionError)
        }
    )

    // Delete Line Error Dialog
    ErrorDialog(
        message = state.deleteLineError,
        title = "Error al Eliminar",
        onDismiss = {
            viewModel.onEvent(BillingEvent.DismissDeleteLineError)
        }
    )

    // Change Quantity Dialog
    ChangeQuantityDialog(
        isVisible = state.showChangeQuantityDialog,
        itemName = state.changeQuantityItemName,
        newQuantity = state.changeQuantityNewQty,
        onQuantityChange = { viewModel.onEvent(BillingEvent.ChangeQuantityValueChanged(it)) },
        onConfirm = { viewModel.onEvent(BillingEvent.ConfirmChangeQuantity) },
        onDismiss = { viewModel.onEvent(BillingEvent.DismissChangeQuantityDialog) },
        isLoading = state.isChangingQuantity
    )

    // Change Quantity Error Dialog
    ErrorDialog(
        message = state.changeQuantityError,
        title = "Error al Cambiar Cantidad",
        onDismiss = {
            viewModel.onEvent(BillingEvent.DismissChangeQuantityError)
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

    // Packaging Dialog
    if (state.showPackagingDialog) {
        PackagingDialog(
            packagingItems = state.packagingItems,
            packagingInputs = state.packagingInputs,
            isLoading = state.isLoadingPackagings,
            isUpdating = state.isUpdatingPackagings,
            error = state.loadPackagingsError ?: state.updatePackagingsError,
            onQuantityChanged = { itemPosId, quantity ->
                viewModel.onEvent(BillingEvent.PackagingQuantityChanged(itemPosId, quantity))
            },
            onSubmit = { viewModel.onEvent(BillingEvent.SubmitPackagings) },
            onDismiss = { viewModel.onEvent(BillingEvent.DismissPackagingDialog) },
            onDismissError = { viewModel.onEvent(BillingEvent.DismissPackagingsError) }
        )
    }

    // Handle navigation after successful pause
    LaunchedEffect(state.shouldNavigateAfterPause) {
        if (state.shouldNavigateAfterPause) {
            viewModel.onEvent(BillingEvent.PauseNavigationHandled)
            onBack()
        }
    }

    // Handle navigation after successful abort
    LaunchedEffect(state.shouldNavigateAfterAbort) {
        if (state.shouldNavigateAfterAbort) {
            viewModel.onEvent(BillingEvent.AbortNavigationHandled)
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
            // Block ALL key events while adding article - don't process, just consume
            if (state.isAddingArticle) {
                return@onPreviewKeyEvent true
            }

            // Process hardware scanner input (Zebra/PAX)
            // Scanner has priority - uses timing to distinguish from manual input
            val barcode = scannerHandler.processKeyEvent(keyEvent)
            if (barcode != null) {
                viewModel.onEvent(BillingEvent.ScannerInput(barcode))
            }
            scannerHandler.shouldConsumeEvent(keyEvent)
        }
    ) { paddingValues ->
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
                    ),
                    enabled = !state.isAddingArticle
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
                        fontSize = dimensions.fontSizeSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cliente: ${state.selectedCustomer?.name ?: "---"}",
                        fontSize = dimensions.fontSizeSmall,
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
                        val hasPackagingItems = state.invoiceData.items.any { it.hasPackaging && !it.isDeleted }

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
                            enabled = hasActiveTransaction && hasPackagingItems,
                            onClick = {
                                showTransactionMenu = false
                                viewModel.onEvent(BillingEvent.OpenPackagingDialog)
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

            Spacer(modifier = Modifier.height(dimensions.spacerSmall))

            // Article search field
            OutlinedTextField(
                value = state.articleSearchQuery,
                onValueChange = { viewModel.onEvent(BillingEvent.ArticleSearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = dimensions.horizontalPadding)
                    .onFocusChanged { focusState ->
                        isArticleFieldFocused = focusState.isFocused
                    },
                placeholder = { Text("Articulo", fontSize = dimensions.fontSizeSmall) },
                enabled = !state.isAddingArticle,
                trailingIcon = {
                    if (state.articleSearchQuery.isNotEmpty() && !state.isAddingArticle) {
                        IconButton(
                            onClick = { viewModel.onEvent(BillingEvent.ArticleSearchQueryChanged("")) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = Color.Gray
                            )
                        }
                    }
                },
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
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = dimensions.fontSizeSmall),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MegaSuperRed,
                    cursorColor = MegaSuperRed
                )
            )

            if (state.isAddingArticle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MegaSuperRed,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimensions.spacerSmall))

            // Items table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Item",
                    fontSize = dimensions.fontSizeSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Cant.",
                    fontSize = dimensions.fontSizeSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Unit.",
                    fontSize = dimensions.fontSizeSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "Total",
                    fontSize = dimensions.fontSizeSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = dimensions.horizontalPadding, vertical = 4.dp),
                color = Color.LightGray
            )

            // Calculate which items should be shown with strikethrough (deleted + orphaned packaging)
            val visuallyDeletedLineSeqs = remember(state.invoiceData.items) {
                val items = state.invoiceData.items
                val result = mutableSetOf<Int>()

                // 1. Items explicitly deleted
                items.filter { it.isDeleted }.forEach { result.add(it.lineItemSequence) }

                // 2. Orphaned packaging items (their nearest parent is deleted)
                items.forEach { item ->
                    // Check if this item is a packaging (some parent references it)
                    val parentItems = items.filter { it.packagingItemId == item.itemId }
                    if (parentItems.isNotEmpty()) {
                        // This is a packaging item - find the nearest parent before this item
                        val nearestParent = parentItems
                            .filter { it.lineItemSequence < item.lineItemSequence }
                            .maxByOrNull { it.lineItemSequence }

                        if (nearestParent?.isDeleted == true) {
                            result.add(item.lineItemSequence)
                        }
                    }
                }

                result
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensions.horizontalPadding)
            ) {
                items(state.invoiceData.items) { item ->
                    val isVisuallyDeleted = visuallyDeletedLineSeqs.contains(item.lineItemSequence)
                    ItemRow(
                        item = item,
                        numberFormat = numberFormat,
                        isSelected = selectedItemId == item.itemId && !isVisuallyDeleted,
                        isVisuallyDeleted = isVisuallyDeleted,
                        onClick = {
                            if (!isVisuallyDeleted) {
                                selectedItemId = if (selectedItemId == item.itemId) null else item.itemId
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                    .padding(vertical = dimensions.spacerSmall)
            ) {
                TotalRow(
                    label = "Subtotal",
                    value = numberFormat.format(state.invoiceData.totals.subTotal)
                )
                Spacer(modifier = Modifier.height(2.dp))
                TotalRow(
                    label = "Impuestos",
                    value = numberFormat.format(state.invoiceData.totals.tax)
                )
                Spacer(modifier = Modifier.height(2.dp))
                TotalRow(
                    label = "Total Ahorrado",
                    value = numberFormat.format(state.invoiceData.totals.totalSavings)
                )
                Spacer(modifier = Modifier.height(4.dp))
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
    isVisuallyDeleted: Boolean = false,
    onClick: () -> Unit = {}
) {
    val dimensions = LocalDimensions.current
    val hasIndicators = item.hasDiscount || item.isSponsor || item.isTaxExempt || item.hasPackaging

    // Styling based on deleted state
    val textColor = if (isVisuallyDeleted) Color.Gray else Color.Black
    val textDecoration = if (isVisuallyDeleted) TextDecoration.LineThrough else TextDecoration.None
    val backgroundColor = when {
        isVisuallyDeleted -> Color(0xFFFAFAFA)  // Light gray for deleted items
        isSelected -> Color(0xFFFFCDD2)  // Light red matching MegaSuper palette
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = !isVisuallyDeleted) { onClick() }
            .padding(vertical = 2.dp, horizontal = 8.dp)
    ) {
        // Fila principal: nombre, cantidad, precio, total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.itemName,
                fontSize = dimensions.fontSizeSmall,
                color = textColor,
                textDecoration = textDecoration,
                modifier = Modifier.weight(2f)
            )
            Text(
                text = item.quantity.toInt().toString(),
                fontSize = dimensions.fontSizeSmall,
                color = textColor,
                textDecoration = textDecoration,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = numberFormat.format(item.unitPrice),
                fontSize = dimensions.fontSizeSmall,
                color = textColor,
                textDecoration = textDecoration,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = numberFormat.format(item.total),
                fontSize = dimensions.fontSizeSmall,
                color = textColor,
                textDecoration = textDecoration,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        if (hasIndicators && !isVisuallyDeleted) {
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
            fontSize = dimensions.fontSizeSmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = if (isBold) dimensions.fontSizeMedium else dimensions.fontSizeSmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
    }
}
