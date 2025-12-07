package com.devlosoft.megaposmobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MegaSuperColorScheme = lightColorScheme(
    primary = MegaSuperRed,
    onPrimary = MegaSuperWhite,
    secondary = MegaSuperRedLight,
    onSecondary = MegaSuperWhite,
    tertiary = MegaSuperRedDark,
    onTertiary = MegaSuperWhite,
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MegaPosMobileTheme(
    content: @Composable () -> Unit
) {
    ProvideDimensions {
        MaterialTheme(
            colorScheme = MegaSuperColorScheme,
            typography = Typography,
            content = content
        )
    }
}