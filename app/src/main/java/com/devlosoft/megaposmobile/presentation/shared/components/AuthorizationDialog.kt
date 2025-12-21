package com.devlosoft.megaposmobile.presentation.shared.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Authorization dialog state
 */
data class AuthorizationDialogState(
    val isVisible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val actionButtonText: String = "",
    val processCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Reusable authorization dialog component for processes where access is denied
 * and authorization from another user is required.
 *
 * @param state The current dialog state
 * @param onAuthorize Callback when user submits credentials. Should return Result.success(Unit) on success,
 *                    or Result.failure(Exception) with the error message on failure.
 * @param onDismiss Callback when dialog is dismissed
 * @param onClearError Callback to clear the error state
 */
@Composable
fun AuthorizationDialog(
    state: AuthorizationDialogState,
    onAuthorize: (userCode: String, password: String) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit = {}
) {
    if (!state.isVisible) return

    var userCode by remember(state.isVisible) { mutableStateOf("") }
    var password by remember(state.isVisible) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = {
            Text(text = state.title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.message,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userCode,
                    onValueChange = {
                        userCode = it
                        if (state.error != null) onClearError()
                    },
                    label = { Text("Usuario Autoriza") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (state.error != null) onClearError()
                    },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Error message
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                // Loading indicator
                if (state.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAuthorize(userCode, password) },
                enabled = !state.isLoading && userCode.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935) // Red color for action
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = state.actionButtonText,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}
