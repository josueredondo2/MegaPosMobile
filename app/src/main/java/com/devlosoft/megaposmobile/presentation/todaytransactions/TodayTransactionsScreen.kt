package com.devlosoft.megaposmobile.presentation.todaytransactions

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.domain.model.TodayTransaction
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.ErrorDialog
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.presentation.shared.components.SuccessDialog
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayTransactionsScreen(
    viewModel: TodayTransactionsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    // Date formatting for header
    val todayDate = remember {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        dateFormat.format(Date())
    }

    // Pagination: Load more when scrolling to bottom
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && state.hasMorePages && !state.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore }
            .collect { shouldLoad ->
                if (shouldLoad) {
                    viewModel.onEvent(TodayTransactionsEvent.LoadNextPage)
                }
            }
    }

    // Dialogs
    ErrorDialog(
        message = state.error ?: state.printError,
        onDismiss = { viewModel.onEvent(TodayTransactionsEvent.DismissError) }
    )

    SuccessDialog(
        isVisible = state.printSuccess,
        message = "Reimpresión exitosa",
        onDismiss = { viewModel.onEvent(TodayTransactionsEvent.DismissPrintSuccess) }
    )

    // Loading overlay for printing
    if (state.isPrinting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MegaSuperRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reimprimiendo...", fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppHeader(endContent = HeaderEndContent.StaticUserIcon)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensions.paddingMedium)
            ) {
                Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                // Title row with date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transacciones",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fecha: $todayDate",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Total count badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Total: ${state.totalCount}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                // Search type radio buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(dimensions.paddingMedium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = state.searchType == SearchType.BY_ID,
                                        onClick = {
                                            viewModel.onEvent(TodayTransactionsEvent.SearchTypeChanged(SearchType.BY_ID))
                                        },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.searchType == SearchType.BY_ID,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MegaSuperRed
                                    )
                                )
                                Text(
                                    text = "Por Id",
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = state.searchType == SearchType.BY_CUSTOMER,
                                        onClick = {
                                            viewModel.onEvent(TodayTransactionsEvent.SearchTypeChanged(SearchType.BY_CUSTOMER))
                                        },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.searchType == SearchType.BY_CUSTOMER,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MegaSuperRed
                                    )
                                )
                                Text(
                                    text = "Por Cliente",
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search field
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = {
                                viewModel.onEvent(TodayTransactionsEvent.SearchQueryChanged(it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color.Gray
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MegaSuperRed,
                                focusedLabelColor = MegaSuperRed,
                                cursorColor = MegaSuperRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                // Loading state
                if (state.isLoading && state.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MegaSuperRed)
                    }
                } else if (state.paginatedTransactions.isEmpty() && !state.isLoading) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty())
                                "No se encontraron transacciones"
                            else
                                "No hay transacciones hoy",
                            color = Color.Gray
                        )
                    }
                } else {
                    // Transaction list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.paginatedTransactions,
                            key = { it.transactionId }
                        ) { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                onReprint = {
                                    viewModel.onEvent(TodayTransactionsEvent.ReprintTransaction(transaction.transactionId))
                                }
                            )
                        }

                        // Loading indicator at bottom
                        if (state.hasMorePages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MegaSuperRed,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TodayTransaction,
    onReprint: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.transactionId,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cliente: ${transaction.displayCustomerName}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reimprimir") },
                        onClick = {
                            menuExpanded = false
                            onReprint()
                        }
                    )
                }
            }
        }
    }
}
