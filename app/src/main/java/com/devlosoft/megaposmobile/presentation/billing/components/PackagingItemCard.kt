package com.devlosoft.megaposmobile.presentation.billing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed

/**
 * Card composable for displaying a packaging item with editable quantity.
 * Design matches the mockup: description at top, chips below, input field at bottom.
 *
 * @param item The packaging item data
 * @param inputValue Current value of the quantity input field
 * @param onQuantityChanged Callback when quantity input changes
 * @param enabled Whether the input field is enabled
 */
@Composable
fun PackagingItemCard(
    item: PackagingItem,
    inputValue: String,
    onQuantityChanged: (String) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Packaging description
        Text(
            text = item.description,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Info chips row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Facturado chip - shows qU_PND (quantityInvoiced)
            InfoChip(
                label = "Facturado",
                value = item.quantityInvoiced.toInt().toString()
            )

            // A cobrar chip
            InfoChip(
                label = "A cobrar",
                value = item.quantityToCharge.toInt().toString()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Entregado input field - full width below chips
        OutlinedTextField(
            value = inputValue,
            onValueChange = { value ->
                // Only allow positive integers
                if (value.isEmpty() || value.matches(Regex("^\\d+$"))) {
                    onQuantityChanged(value)
                }
            },
            label = { Text("Entregado") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MegaSuperRed,
                focusedLabelColor = MegaSuperRed,
                cursorColor = MegaSuperRed
            )
        )
    }
}

/**
 * Simple bordered chip for displaying label-value pairs.
 * Matches the mockup design with light border and dark text.
 */
@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}
