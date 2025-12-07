package com.devlosoft.megaposmobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enum para clasificar el tipo de dispositivo según el ancho de pantalla
 */
enum class DeviceType {
    PHONE,      // < 600dp (teléfonos pequeños y medianos)
    PHABLET,    // 600dp - 839dp (teléfonos grandes / tablets pequeñas)
    TABLET      // >= 840dp (tablets)
}

/**
 * Clase que contiene todas las dimensiones adaptativas
 */
data class Dimensions(
    // Padding
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val paddingExtraLarge: Dp,

    // Content width
    val maxContentWidth: Dp,
    val horizontalPadding: Dp,

    // Spacing
    val spacerSmall: Dp,
    val spacerMedium: Dp,
    val spacerLarge: Dp,
    val spacerExtraLarge: Dp,

    // Font sizes
    val fontSizeSmall: TextUnit,
    val fontSizeMedium: TextUnit,
    val fontSizeLarge: TextUnit,
    val fontSizeExtraLarge: TextUnit,
    val fontSizeTitle: TextUnit,
    val fontSizeHeader: TextUnit,

    // Component sizes
    val buttonHeight: Dp,
    val textFieldHeight: Dp,
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,
    val headerHeight: Dp,

    // Logo/Brand
    val logoFontSize: TextUnit,
    val sloganFontSize: TextUnit,

    // Device type
    val deviceType: DeviceType
)

/**
 * Dimensiones para teléfonos (< 600dp)
 */
private val phoneDimensions = Dimensions(
    paddingSmall = 8.dp,
    paddingMedium = 16.dp,
    paddingLarge = 24.dp,
    paddingExtraLarge = 32.dp,

    maxContentWidth = Dp.Infinity,
    horizontalPadding = 32.dp,

    spacerSmall = 8.dp,
    spacerMedium = 16.dp,
    spacerLarge = 32.dp,
    spacerExtraLarge = 48.dp,

    fontSizeSmall = 12.sp,
    fontSizeMedium = 14.sp,
    fontSizeLarge = 16.sp,
    fontSizeExtraLarge = 18.sp,
    fontSizeTitle = 24.sp,
    fontSizeHeader = 28.sp,

    buttonHeight = 56.dp,
    textFieldHeight = 60.dp,
    iconSizeSmall = 18.dp,
    iconSizeMedium = 24.dp,
    iconSizeLarge = 64.dp,
    headerHeight = 80.dp,

    logoFontSize = 24.sp,
    sloganFontSize = 10.sp,

    deviceType = DeviceType.PHONE
)

/**
 * Dimensiones para phablets/tablets pequeñas (600dp - 839dp)
 */
private val phabletDimensions = Dimensions(
    paddingSmall = 12.dp,
    paddingMedium = 20.dp,
    paddingLarge = 32.dp,
    paddingExtraLarge = 40.dp,

    maxContentWidth = 500.dp,
    horizontalPadding = 48.dp,

    spacerSmall = 12.dp,
    spacerMedium = 20.dp,
    spacerLarge = 40.dp,
    spacerExtraLarge = 56.dp,

    fontSizeSmall = 14.sp,
    fontSizeMedium = 16.sp,
    fontSizeLarge = 18.sp,
    fontSizeExtraLarge = 20.sp,
    fontSizeTitle = 28.sp,
    fontSizeHeader = 32.sp,

    buttonHeight = 60.dp,
    textFieldHeight = 60.dp,
    iconSizeSmall = 20.dp,
    iconSizeMedium = 28.dp,
    iconSizeLarge = 72.dp,
    headerHeight = 100.dp,

    logoFontSize = 28.sp,
    sloganFontSize = 12.sp,

    deviceType = DeviceType.PHABLET
)

/**
 * Dimensiones para tablets (>= 840dp)
 */
private val tabletDimensions = Dimensions(
    paddingSmall = 16.dp,
    paddingMedium = 24.dp,
    paddingLarge = 40.dp,
    paddingExtraLarge = 56.dp,

    maxContentWidth = 600.dp,
    horizontalPadding = 64.dp,

    spacerSmall = 16.dp,
    spacerMedium = 24.dp,
    spacerLarge = 48.dp,
    spacerExtraLarge = 64.dp,

    fontSizeSmall = 16.sp,
    fontSizeMedium = 18.sp,
    fontSizeLarge = 20.sp,
    fontSizeExtraLarge = 24.sp,
    fontSizeTitle = 36.sp,
    fontSizeHeader = 40.sp,

    buttonHeight = 64.dp,
    textFieldHeight = 64.dp,
    iconSizeSmall = 24.dp,
    iconSizeMedium = 32.dp,
    iconSizeLarge = 80.dp,
    headerHeight = 120.dp,

    logoFontSize = 36.sp,
    sloganFontSize = 14.sp,

    deviceType = DeviceType.TABLET
)

/**
 * CompositionLocal para acceder a las dimensiones desde cualquier parte
 */
val LocalDimensions = compositionLocalOf { phoneDimensions }

/**
 * Función para obtener las dimensiones según el ancho de pantalla
 */
@Composable
fun rememberDimensions(): Dimensions {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    return remember(screenWidthDp) {
        when {
            screenWidthDp < 600 -> phoneDimensions
            screenWidthDp < 840 -> phabletDimensions
            else -> tabletDimensions
        }
    }
}

/**
 * Provider que envuelve el contenido con las dimensiones correctas
 */
@Composable
fun ProvideDimensions(content: @Composable () -> Unit) {
    val dimensions = rememberDimensions()
    CompositionLocalProvider(LocalDimensions provides dimensions) {
        content()
    }
}
