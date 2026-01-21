package com.devlosoft.megaposmobile.presentation.process

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.presentation.shared.components.AppHeader
import com.devlosoft.megaposmobile.presentation.shared.components.HeaderEndContent
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import com.devlosoft.megaposmobile.ui.theme.MegaSuperWhite

// Colors for process states
private val SuccessGreen = Color(0xFF4CAF50)
private val ErrorOrange = Color(0xFFFF9800)
private val BackgroundGray = Color(0xFFF5F5F5)

@Composable
fun ProcessScreen(
    processType: String,
    viewModel: ProcessViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null,
    onSuccess: (() -> Unit)? = null,
    onSkipPrintSuccess: (() -> Unit)? = null,
    autoStartProcess: Boolean = true,
    successButtonText: String = "Volver a menu",
    errorBackButtonText: String = "Volver a menu"
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    // Track if navigation is in progress to prevent multiple clicks
    var isNavigating by remember { mutableStateOf(false) }

    Log.d("ProcessScreen", "ProcessScreen composed - processType: $processType, status: ${state.status}, isNavigating: $isNavigating")

    // Disable back button - navigation only through screen buttons
    BackHandler(enabled = true) {
        Log.d("ProcessScreen", "Hardware back button pressed - blocked")
        // Do nothing - block back navigation
    }

    // Start the process when screen is launched
    LaunchedEffect(processType) {
        if (autoStartProcess) {
            viewModel.startProcess(processType)
        }
    }

    @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Header
            AppHeader(
                endContent = HeaderEndContent.StaticUserIcon
            )

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = dimensions.maxContentWidth)
                        .padding(horizontal = dimensions.horizontalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (val status = state.status) {
                        is ProcessStatus.Loading -> {
                            LoadingContent(
                                message = state.loadingMessage
                            )
                        }
                        is ProcessStatus.Success -> {
                            SuccessContent(
                                message = status.message,
                                onBackClick = {
                                    if (!isNavigating) {
                                        isNavigating = true
                                        Log.d("ProcessScreen", "=== SUCCESS BACK BUTTON PRESSED ===")
                                        Log.d("ProcessScreen", "processType: $processType")
                                        Log.d("ProcessScreen", "current status: ${state.status}")
                                        try {
                                            (onSuccess ?: onBack).invoke()
                                        } catch (e: Exception) {
                                            Log.e("ProcessScreen", "ERROR during success navigation", e)
                                            isNavigating = false
                                        }
                                    } else {
                                        Log.w("ProcessScreen", "Navigation already in progress, ignoring success click")
                                    }
                                },
                                buttonText = successButtonText,
                                isEnabled = !isNavigating,
                                onReprintClick = if (state.dataphoneCloseReceiptText != null) {
                                    { viewModel.reprintDataphoneClose() }
                                } else null
                            )
                        }
                        is ProcessStatus.Error -> {
                            ErrorContent(
                                message = status.message,
                                onRetryClick = onRetry,
                                onBackClick = {
                                    if (!isNavigating) {
                                        isNavigating = true
                                        Log.d("ProcessScreen", "=== ERROR BACK BUTTON PRESSED ===")
                                        Log.d("ProcessScreen", "processType: $processType")
                                        Log.d("ProcessScreen", "current status: ${state.status}")
                                        try {
                                            onBack.invoke()
                                        } catch (e: Exception) {
                                            Log.e("ProcessScreen", "ERROR during error navigation", e)
                                            isNavigating = false
                                        }
                                    } else {
                                        Log.w("ProcessScreen", "Navigation already in progress, ignoring error click")
                                    }
                                },
                                backButtonText = errorBackButtonText,
                                isEnabled = !isNavigating
                            )
                        }
                        is ProcessStatus.PrintError -> {
                            PrintErrorContent(
                                message = status.message,
                                onRetryPrintClick = { viewModel.retryPrint() },
                                onSkipPrintClick = {
                                    // Navigate directly to new transaction if callback provided
                                    if (onSkipPrintSuccess != null) {
                                        onSkipPrintSuccess()
                                    } else {
                                        viewModel.skipPrint()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    val dimensions = LocalDimensions.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated red circular spinner
        CircularProgressIndicator(
            modifier = Modifier.size(120.dp),
            color = MegaSuperRed,
            strokeWidth = 6.dp,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(dimensions.spacerLarge))

        // Loading message
        Text(
            text = message,
            fontSize = dimensions.fontSizeMedium,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessContent(
    message: String,
    onBackClick: () -> Unit,
    buttonText: String = "Volver a menu",
    isEnabled: Boolean = true,
    onReprintClick: (() -> Unit)? = null
) {
    val dimensions = LocalDimensions.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Green checkmark icon
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = SuccessGreen,
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(dimensions.spacerMedium))

        // Success message
        Text(
            text = message,
            fontSize = dimensions.fontSizeMedium,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensions.spacerExtraLarge))

        // Reprint button (only shown if onReprintClick is provided)
        if (onReprintClick != null) {
            Button(
                onClick = onReprintClick,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensions.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Reimprimir",
                    fontSize = dimensions.fontSizeExtraLarge,
                    fontWeight = FontWeight.Medium,
                    color = MegaSuperWhite
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacerMedium))
        }

        // Action button
        Button(
            onClick = onBackClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (onReprintClick != null) Color.Gray else MegaSuperRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = dimensions.fontSizeExtraLarge,
                fontWeight = FontWeight.Medium,
                color = MegaSuperWhite
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetryClick: (() -> Unit)? = null,
    onBackClick: () -> Unit,
    backButtonText: String = "Volver a menu",
    isEnabled: Boolean = true
) {
    val dimensions = LocalDimensions.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Orange circle with exclamation mark
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(ErrorOrange),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = "Error",
                tint = MegaSuperWhite,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacerMedium))

        // Error message
        Text(
            text = message,
            fontSize = dimensions.fontSizeMedium,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensions.spacerExtraLarge))

        // Retry button (only shown if onRetryClick is provided)
        if (onRetryClick != null) {
            Button(
                onClick = onRetryClick,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensions.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MegaSuperRed
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Reintentar pago",
                    fontSize = dimensions.fontSizeExtraLarge,
                    fontWeight = FontWeight.Medium,
                    color = MegaSuperWhite
                )
            }

            Spacer(modifier = Modifier.height(dimensions.spacerMedium))
        }

        // Back button
        Button(
            onClick = onBackClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (onRetryClick != null) Color.Gray else MegaSuperRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = backButtonText,
                fontSize = dimensions.fontSizeExtraLarge,
                fontWeight = FontWeight.Medium,
                color = MegaSuperWhite
            )
        }
    }
}

@Composable
private fun PrintErrorContent(
    message: String,
    onRetryPrintClick: () -> Unit,
    onSkipPrintClick: () -> Unit
) {
    val dimensions = LocalDimensions.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Orange circle with exclamation mark
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(ErrorOrange),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = "Print Error",
                tint = MegaSuperWhite,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacerMedium))

        // Title
        Text(
            text = "Error de Impresión",
            fontSize = dimensions.fontSizeLarge,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensions.spacerSmall))

        // Error message
        Text(
            text = message,
            fontSize = dimensions.fontSizeMedium,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensions.spacerSmall))

        // Info message
        Text(
            text = "La transacción fue procesada exitosamente",
            fontSize = dimensions.fontSizeSmall,
            fontWeight = FontWeight.Normal,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(dimensions.spacerExtraLarge))

        // Retry print button
        Button(
            onClick = onRetryPrintClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MegaSuperRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Reintentar impresión",
                fontSize = dimensions.fontSizeExtraLarge,
                fontWeight = FontWeight.Medium,
                color = MegaSuperWhite
            )
        }

        Spacer(modifier = Modifier.height(dimensions.spacerMedium))

        // Skip print button (continue to new transaction)
        Button(
            onClick = onSkipPrintClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Nueva transacción",
                fontSize = dimensions.fontSizeExtraLarge,
                fontWeight = FontWeight.Medium,
                color = MegaSuperWhite
            )
        }
    }
}
