package com.devlosoft.megaposmobile.presentation.process

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    autoStartProcess: Boolean = true
) {
    val state by viewModel.state.collectAsState()
    val dimensions = LocalDimensions.current

    // Start the process when screen is launched
    LaunchedEffect(processType) {
        if (autoStartProcess) {
            viewModel.startProcess(processType)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundGray)
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
                                onBackClick = onBack
                            )
                        }
                        is ProcessStatus.Error -> {
                            ErrorContent(
                                message = status.message,
                                onBackClick = onBack
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
    onBackClick: () -> Unit
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

        // Back to menu button
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MegaSuperRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Volver a menu",
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
    onBackClick: () -> Unit
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

        // Back to menu button
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MegaSuperRed
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Volver a menu",
                fontSize = dimensions.fontSizeExtraLarge,
                fontWeight = FontWeight.Medium,
                color = MegaSuperWhite
            )
        }
    }
}
