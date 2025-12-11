package com.devlosoft.megaposmobile.presentation.shared.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.devlosoft.megaposmobile.R
import com.devlosoft.megaposmobile.ui.theme.LocalDimensions
import com.devlosoft.megaposmobile.ui.theme.MegaSuperRed
import com.devlosoft.megaposmobile.ui.theme.MegaSuperWhite

/**
 * Define el contenido del lado derecho del header
 */
sealed class HeaderEndContent {
    /**
     * Menú dropdown con opciones de usuario
     */
    data class UserMenu(
        val items: List<MenuItem>,
        val expanded: Boolean? = null,
        val onExpandedChange: ((Boolean) -> Unit)? = null
    ) : HeaderEndContent()

    /**
     * Icono de usuario estático sin interacción
     */
    data object StaticUserIcon : HeaderEndContent()

    /**
     * Texto de versión de la aplicación
     */
    data class VersionText(
        val version: String = "1.0"
    ) : HeaderEndContent()
}

/**
 * Item individual del menú dropdown
 */
data class MenuItem(
    val text: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)

/**
 * Header universal de la aplicación MegaSuper
 *
 * @param modifier Modificador opcional
 * @param endContent Tipo de contenido a mostrar en el lado derecho
 */
@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    endContent: HeaderEndContent = HeaderEndContent.StaticUserIcon
) {
    val dimensions = LocalDimensions.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MegaSuperRed)
            .height(dimensions.headerHeight)
            .padding(horizontal = dimensions.paddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo (siempre presente)
            Image(
                painter = painterResource(id = R.drawable.logo_megasuper),
                contentDescription = "MegaSuper Logo",
                modifier = Modifier.height(dimensions.headerHeight * 0.6f),
                contentScale = ContentScale.FillHeight
            )

            // Contenido derecho según el tipo
            when (endContent) {
                is HeaderEndContent.UserMenu -> {
                    UserMenuSection(
                        items = endContent.items,
                        expanded = endContent.expanded,
                        onExpandedChange = endContent.onExpandedChange
                    )
                }
                is HeaderEndContent.StaticUserIcon -> {
                    StaticUserIconSection()
                }
                is HeaderEndContent.VersionText -> {
                    VersionTextSection(version = endContent.version)
                }
            }
        }
    }
}

@Composable
private fun UserMenuSection(
    items: List<MenuItem>,
    expanded: Boolean?,
    onExpandedChange: ((Boolean) -> Unit)?
) {
    val dimensions = LocalDimensions.current

    // Estado local si no se proporciona desde fuera
    var localExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: localExpanded
    val setExpanded: (Boolean) -> Unit = { newValue ->
        if (expanded == null) {
            localExpanded = newValue
        }
        onExpandedChange?.invoke(newValue)
    }

    Box {
        IconButton(onClick = { setExpanded(!isExpanded) }) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Usuario",
                tint = MegaSuperWhite,
                modifier = Modifier.size(dimensions.iconSizeLarge * 0.6f)
            )
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { setExpanded(false) }
        ) {
            items.forEach { menuItem ->
                DropdownMenuItem(
                    text = { Text(menuItem.text) },
                    onClick = {
                        setExpanded(false)
                        menuItem.onClick()
                    },
                    leadingIcon = menuItem.icon?.let { icon ->
                        {
                            Icon(
                                imageVector = icon,
                                contentDescription = menuItem.text
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StaticUserIconSection() {
    val dimensions = LocalDimensions.current

    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = "Usuario",
        tint = MegaSuperWhite,
        modifier = Modifier.size(dimensions.iconSizeLarge * 0.6f)
    )
}

@Composable
private fun VersionTextSection(version: String) {
    val dimensions = LocalDimensions.current

    Text(
        text = "Version: $version",
        color = MegaSuperWhite,
        fontSize = dimensions.fontSizeMedium
    )
}
