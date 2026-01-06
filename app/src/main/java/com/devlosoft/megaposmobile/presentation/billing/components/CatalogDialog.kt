package com.devlosoft.megaposmobile.presentation.billing.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devlosoft.megaposmobile.domain.model.CatalogItem
import com.devlosoft.megaposmobile.domain.model.CatalogType
import com.devlosoft.megaposmobile.presentation.billing.state.CatalogDialogState
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed

@Composable
fun CatalogDialog(
    state: CatalogDialogState,
    onCategorySelected: (Int) -> Unit,
    onLetterSelected: (Char) -> Unit,
    onItemSelected: (CatalogItem) -> Unit,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!state.isAddingItem) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !state.isAddingItem,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                CatalogHeader(onDismiss = onDismiss, isEnabled = !state.isAddingItem)

                // Letter Bar (A-Z)
                LetterBar(
                    selectedLetter = state.selectedLetter,
                    onLetterSelected = onLetterSelected,
                    enabled = !state.isLoadingItems && !state.isAddingItem
                )

                // Category Tabs
                CategoryTabs(
                    categories = state.catalogTypes,
                    selectedCategoryId = state.selectedCatalogTypeId,
                    onCategorySelected = onCategorySelected,
                    isLoading = state.isLoadingTypes,
                    enabled = !state.isLoadingItems && !state.isAddingItem
                )

                // Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    when {
                        state.error != null -> ErrorContent(state.error, onDismissError)
                        state.isLoadingItems -> LoadingContent("Cargando productos...")
                        state.catalogItems.isEmpty() && state.selectedCatalogTypeId != null -> EmptyContent()
                        state.selectedCatalogTypeId == null -> SelectCategoryContent()
                        else -> ProductGrid(
                            items = state.catalogItems,
                            onItemClick = onItemSelected,
                            enabled = !state.isAddingItem
                        )
                    }

                    // Overlay when adding item
                    if (state.isAddingItem) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MegaSuperRed)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Agregando producto...", fontWeight = FontWeight.Medium)
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
private fun CatalogHeader(onDismiss: () -> Unit, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Catalogo Digital",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        IconButton(onClick = onDismiss, enabled = isEnabled) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = if (isEnabled) Color.Gray else Color.LightGray
            )
        }
    }
}

@Composable
private fun LetterBar(
    selectedLetter: Char,
    onLetterSelected: (Char) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CatalogDialogState.LETTERS.forEach { letter ->
            FilterChip(
                selected = selectedLetter == letter,
                onClick = { if (enabled) onLetterSelected(letter) },
                label = { Text(letter.toString(), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MegaSuperRed,
                    selectedLabelColor = Color.White
                ),
                enabled = enabled
            )
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<CatalogType>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int) -> Unit,
    isLoading: Boolean,
    enabled: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MegaSuperRed
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategoryId == category.catalogTypeId
                Button(
                    onClick = { if (enabled) onCategorySelected(category.catalogTypeId) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MegaSuperRed else Color.LightGray,
                        contentColor = if (isSelected) Color.White else Color.Black,
                        disabledContainerColor = if (isSelected) MegaSuperRed.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.5f),
                        disabledContentColor = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    enabled = enabled
                ) {
                    Text(
                        text = category.catalogName,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(
    items: List<CatalogItem>,
    onItemClick: (CatalogItem) -> Unit,
    enabled: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.itemPosId }) { item ->
            ProductCard(
                item = item,
                onClick = { if (enabled) onItemClick(item) }
            )
        }
    }
}

@Composable
private fun ProductCard(
    item: CatalogItem,
    onClick: () -> Unit
) {
    // Decode Base64 image
    val bitmap = remember(item.catalogItemImage) {
        item.catalogItemImage?.let { base64String ->
            try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = item.catalogItemName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ImageNotSupported,
                        contentDescription = "Sin imagen",
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Product name
            Text(
                text = item.catalogItemName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MegaSuperRed)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun ErrorContent(message: String, onDismissError: () -> Unit) {
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
            text = message,
            fontSize = 14.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onDismissError) {
            Text("Reintentar", color = MegaSuperRed)
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No hay productos",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Seleccione otra categoria o letra",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectCategoryContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Seleccione una categoria",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Escoja una categoria arriba para ver los productos",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
