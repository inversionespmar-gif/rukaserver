package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.PlayerAccent
import com.rukatv.iptv.ui.theme.PlayerSecondary

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) PlayerAccent else Color.White,
            modifier = Modifier.size(20.dp)
        )
        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isActive) PlayerAccent else PlayerSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ScreenshotButton(
    onScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerActionButton(
        icon = Icons.Filled.CameraAlt,
        contentDescription = "Captura",
        label = "Captura",
        onClick = onScreenshot,
        modifier = modifier
    )
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerActionButton(
        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        contentDescription = "Favorito",
        label = "Favorito",
        onClick = onToggle,
        modifier = modifier,
        isActive = isFavorite
    )
}

@Composable
fun PipButton(
    onPipRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerActionButton(
        icon = Icons.Filled.PictureInPictureAlt,
        contentDescription = "PiP",
        label = "PiP",
        onClick = onPipRequested,
        modifier = modifier
    )
}

@Composable
fun SleepTimerButton(
    remainingMinutes: Int?,
    onSelectTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .dpadFocus(interactionSource = interaction)
                .clickable(interactionSource = interaction, indication = null) { showMenu = true }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Temporizador",
                tint = if (remainingMinutes != null) PlayerAccent else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = remainingMinutes?.let { "${it}min" } ?: "Timer",
                color = if (remainingMinutes != null) PlayerAccent else PlayerSecondary,
                fontSize = 10.sp
            )
        }

        if (showMenu) {
            PlayerDropdownMenu(
                visible = showMenu,
                onDismiss = { showMenu = false }
            ) {
                val timerOptions = listOf(
                    null to "Desactivar",
                    15 to "15 minutos",
                    30 to "30 minutos",
                    45 to "45 minutos",
                    60 to "1 hora",
                    120 to "2 horas"
                )
                timerOptions.forEach { (minutes, label) ->
                    PlayerMenuItem(
                        option = MenuOption(
                            id = minutes?.toString() ?: "off",
                            label = label,
                            isSelected = remainingMinutes == minutes
                        ),
                        onSelect = {
                            onSelectTimer(minutes)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
