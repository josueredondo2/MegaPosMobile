package com.devlosoft.megaposmobile.presentation.configuration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import com.devlosoft.megaposmobile.ui.theme.MegaSuperWhite

@Composable
fun ConfigurationScreen(
    viewModel: ConfigurationViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val dimensions = LocalDimensions.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("Configuración guardada exitosamente")
            viewModel.onEvent(ConfigurationEvent.ClearSavedFlag)
            onBack()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(ConfigurationEvent.ClearError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Header
            AppHeader(
                endContent = HeaderEndContent.VersionText(version = "1.0")
            )

            // Contenido centrado
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = dimensions.maxContentWidth)
                        .padding(horizontal = dimensions.horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(dimensions.spacerExtraLarge))

                    // Título
                    Text(
                        text = "POS Mobile",
                        fontSize = dimensions.fontSizeTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerSmall))

                    // Subtítulo
                    Text(
                        text = "Configura esta unidad",
                        fontSize = dimensions.fontSizeLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Campo Dirección POS API
                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = { viewModel.onEvent(ConfigurationEvent.ServerUrlChanged(it)) },
                        label = {
                            Text(
                                text = "Dirección POS API",
                                fontSize = dimensions.fontSizeMedium
                            )
                        },
                        placeholder = {
                            Text(
                                text = "http://[dominio]:puerto",
                                fontSize = dimensions.fontSizeMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.textFieldHeight),
                        enabled = !state.isLoading,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = dimensions.fontSizeMedium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MegaSuperRed,
                            focusedLabelColor = MegaSuperRed,
                            cursorColor = MegaSuperRed
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // Android ID Label (Read-only)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Android ID",
                            fontSize = dimensions.fontSizeSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.androidId.ifBlank { "Obteniendo..." },
                            fontSize = dimensions.fontSizeMedium,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerMedium))

                    // WiFi IP Label (Read-only)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "IP WiFi Local",
                            fontSize = dimensions.fontSizeSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.wifiIp.ifBlank { "Obteniendo..." },
                            fontSize = dimensions.fontSizeMedium,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(dimensions.spacerLarge))

                    // Botón Guardar
                    Button(
                        onClick = { viewModel.onEvent(ConfigurationEvent.Save) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimensions.buttonHeight),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MegaSuperRed,
                            contentColor = MegaSuperWhite
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(dimensions.iconSizeMedium),
                                color = MegaSuperWhite
                            )
                        } else {
                            Text(
                                text = "Guardar",
                                fontSize = dimensions.fontSizeExtraLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
