package com.devlosoft.megaposmobile.presentation.billing.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed

/**
 * Full-screen dialog for managing packaging items.
 *
 * @param packagingItems List of packaging items to display
 * @param packagingInputs Map of itemPosId to input value for each item
 * @param isLoading Whether packaging data is being loaded
 * @param isUpdating Whether packaging update is in progress
 * @param error Error message to display, if any
 * @param onQuantityChanged Callback when quantity input changes for an item
 * @param onSubmit Callback when submit button is clicked
 * @param onDismiss Callback when dialog is dismissed
 * @param onDismissError Callback when error is dismissed
 */
@Composable
fun PackagingDialog(
    packagingItems: List<PackagingItem>,
    packagingInputs: Map<String, String>,
    isLoading: Boolean,
    isUpdating: Boolean,
    error: String?,
    onQuantityChanged: (itemPosId: String, quantity: String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = { if (!isLoading && !isUpdating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading && !isUpdating,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header - white background like mockup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Agregar Envases",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isLoading && !isUpdating
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.Gray
                        )
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            // Loading state
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MegaSuperRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Cargando envases...",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        error != null -> {
                            // Error state
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Error",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MegaSuperRed
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = onDismissError) {
                                    Text("Reintentar", color = MegaSuperRed)
                                }
                            }
                        }

                        packagingItems.isEmpty() -> {
                            // Empty state
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No hay envases disponibles",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Esta transaccion no tiene articulos con envases asociados.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        else -> {
                            // Packaging items list
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = packagingItems,
                                    key = { it.itemPosId }
                                ) { item ->
                                    PackagingItemCard(
                                        item = item,
                                        inputValue = packagingInputs[item.itemPosId] ?: "",
                                        onQuantityChanged = { quantity ->
                                            onQuantityChanged(item.itemPosId, quantity)
                                        },
                                        enabled = !isUpdating
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer with buttons - matching mockup layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Submit button (Actualizar) - red filled, on left like mockup
                    Button(
                        onClick = onSubmit,
                        enabled = !isLoading && !isUpdating && packagingItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Actualizar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    // Cancel button - outlined style like mockup
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading && !isUpdating,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 14.sp,
                            color = if (!isLoading && !isUpdating) Color.DarkGray else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
