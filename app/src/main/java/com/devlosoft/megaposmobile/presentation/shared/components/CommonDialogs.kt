package com.devlosoft.megaposmobile.presentation.shared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                    containerColor = Color(0xFF4CAF50) // Green for success
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
