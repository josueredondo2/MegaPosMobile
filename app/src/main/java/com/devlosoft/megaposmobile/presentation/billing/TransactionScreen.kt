package com.devlosoft.megaposmobile.presentation.billing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devlosoft.megaposmobile.R
import com.devlosoft.megaposmobile.domain.model.InvoiceItem
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import com.devlosoft.megaposmobile.ui.theme.MegaSuperWhite
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onFinalize: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
        maximumFractionDigits = 0
    }

    // Handle navigation back to billing after finalizing
    LaunchedEffect(state.shouldNavigateBackToBilling) {
        if (state.shouldNavigateBackToBilling) {
            try {
                android.util.Log.d("TransactionScreen", "Starting navigation back to billing...")
                viewModel.onEvent(BillingEvent.ResetForNewTransaction)
                android.util.Log.d("TransactionScreen", "ResetForNewTransaction event sent, calling onFinalize...")
                onFinalize()
                android.util.Log.d("TransactionScreen", "onFinalize completed")
            } catch (e: Exception) {
                android.util.Log.e("TransactionScreen", "Error during navigation: ${e.message}", e)
            }
        }
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

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Header
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
                    Image(
                        painter = painterResource(id = R.drawable.logo_megasuper),
                        contentDescription = "MegaSuper Logo",
                        modifier = Modifier.height(dimensions.headerHeight * 0.6f),
                        contentScale = ContentScale.FillHeight
                    )
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Usuario",
                        tint = MegaSuperWhite,
                        modifier = Modifier.size(dimensions.iconSizeLarge * 0.6f)
                    )
                }
            }

            // Transaction info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensions.horizontalPadding)
                    .padding(top = dimensions.spacerMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
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

                // Menu icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Menu",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onEvent(BillingEvent.AddArticle) }
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
                    ItemRow(item = item, numberFormat = numberFormat)
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
                onClick = { viewModel.onEvent(BillingEvent.FinalizeTransaction) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensions.buttonHeight),
                enabled = state.invoiceData.items.isNotEmpty() && !state.isFinalizingTransaction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                if (state.isFinalizingTransaction) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Finalizar",
                        fontSize = dimensions.fontSizeExtraLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: InvoiceItem,
    numberFormat: NumberFormat
) {
    val dimensions = LocalDimensions.current

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
