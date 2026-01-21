package com.devlosoft.megaposmobile.presentation.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devlosoft.megaposmobile.core.constants.FieldLengths
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed

/**
 * Standard error dialog for displaying error messages.
 *
 * @param message The error message to display. Dialog is hidden if null.
 * @param title Optional custom title (defaults to "Error")
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun ErrorDialog(
    message: String?,
    title: String = "Error",
    onDismiss: () -> Unit
) {
    if (message == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        }
    )
}

/**
 * Confirmation dialog with confirm and cancel buttons.
 *
 * @param isVisible Whether the dialog is visible
 * @param title The dialog title
 * @param message The confirmation message
 * @param confirmText Text for confirm button (defaults to "Confirmar")
 * @param dismissText Text for dismiss button (defaults to "Cancelar")
 * @param confirmColor Color for confirm button (defaults to red for destructive actions)
 * @param onConfirm Callback when confirmed
 * @param onDismiss Callback when dismissed
 */
@Composable
fun ConfirmDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    confirmText: String = "Confirmar",
    dismissText: String = "Cancelar",
    confirmColor: Color = Color(0xFFE53935),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = confirmText,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * TODO dialog for features that are not yet implemented.
 *
 * @param isVisible Whether the dialog is visible
 * @param feature The name of the feature (will be shown in message)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun TodoDialog(
    isVisible: Boolean,
    feature: String,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Función en desarrollo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "La función estará disponible próximamente.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = feature,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

/**
 * Success dialog for displaying success messages.
 *
 * @param isVisible Whether the dialog is visible
 * @param title The dialog title (defaults to "Éxito")
 * @param message The success message
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun SuccessDialog(
    isVisible: Boolean,
    title: String = "Éxito",
    message: String,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Aceptar",
                    color = Color.White
                )
            }
        }
    )
}

/**
 * Information dialog for displaying general information.
 *
 * @param isVisible Whether the dialog is visible
 * @param title The dialog title
 * @param message The information message
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun InfoDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Aceptar")
            }
        }
    )
}

/**
 * Abort transaction confirmation dialog with reason text field.
 *
 * @param isVisible Whether the dialog is visible
 * @param reason Current value of the reason text field
 * @param onReasonChanged Callback when reason text changes
 * @param onConfirm Callback when abort is confirmed
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AbortConfirmDialog(
    isVisible: Boolean,
    reason: String,
    onReasonChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val isReasonEmpty = reason.trim().isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Abortar Transacción",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¿Seguro que desea abortar la transacción?",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { newValue ->
                        if (newValue.length <= FieldLengths.ABORT_REASON) {
                            onReasonChanged(newValue)
                        }
                    },
                    label = { Text("Motivo") },
                    placeholder = { Text("Ingrese el motivo de la cancelación") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    isError = isReasonEmpty && reason.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MegaSuperRed,
                        focusedLabelColor = MegaSuperRed,
                        cursorColor = MegaSuperRed
                    )
                )
                if (isReasonEmpty) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "El motivo es requerido",
                        fontSize = 12.sp,
                        color = MegaSuperRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isReasonEmpty,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Abortar",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Change quantity dialog with quantity text field and +/- buttons.
 *
 * @param isVisible Whether the dialog is visible
 * @param itemName Name of the item being modified
 * @param newQuantity Current value of the quantity text field
 * @param onQuantityChange Callback when quantity text changes
 * @param onConfirm Callback when change is confirmed
 * @param onDismiss Callback when dialog is dismissed
 * @param isLoading Whether the change operation is in progress
 */
@Composable
fun ChangeQuantityDialog(
    isVisible: Boolean,
    itemName: String,
    newQuantity: String,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    if (!isVisible) return

    val quantity = newQuantity.toIntOrNull() ?: 0
    val isValidQuantity = quantity >= 1 && quantity <= 99

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Cambiar Cantidad",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Articulo $itemName",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Minus button
                    Button(
                        onClick = {
                            val current = newQuantity.toIntOrNull() ?: 1
                            if (current > 1) {
                                onQuantityChange((current - 1).toString())
                            }
                        },
                        enabled = !isLoading && quantity > 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "−",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Quantity text field
                    OutlinedTextField(
                        value = newQuantity,
                        onValueChange = { value ->
                            // Only allow numeric input (integers)
                            if (value.isEmpty() || value.matches(Regex("^\\d+$"))) {
                                val numValue = value.toIntOrNull()
                                if (numValue == null || numValue <= 99) {
                                    onQuantityChange(value)
                                }
                            }
                        },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = newQuantity.isNotEmpty() && !isValidQuantity,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MegaSuperRed,
                            focusedLabelColor = MegaSuperRed,
                            cursorColor = MegaSuperRed
                        )
                    )

                    // Plus button
                    Button(
                        onClick = {
                            val current = newQuantity.toIntOrNull() ?: 0
                            if (current < 99) {
                                onQuantityChange((current + 1).toString())
                            }
                        },
                        enabled = !isLoading && quantity < 99,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                if (newQuantity.isNotEmpty() && !isValidQuantity) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "La cantidad debe ser entre 1 y 99",
                        fontSize = 12.sp,
                        color = MegaSuperRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isValidQuantity && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        text = "Cambiar",
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}
