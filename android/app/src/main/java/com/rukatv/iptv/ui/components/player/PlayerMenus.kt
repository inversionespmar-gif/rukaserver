package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.PlayerAccent
import com.rukatv.iptv.ui.theme.PlayerSurface

data class MenuOption(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

@Composable
fun PlayerDropdownMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        Box(
            modifier = modifier
                .width(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlayerSurface)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

@Composable
fun PlayerMenuItem(
    option: MenuOption,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onSelect(option.id) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.label,
                color = if (option.isSelected) PlayerAccent else Color.White,
                fontSize = 13.sp,
                fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (option.isSelected) {
                Text("\u2713", color = PlayerAccent, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SpeedMenu(
    visible: Boolean,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(
        MenuOption("0.5", "0.5x", currentSpeed == 0.5f),
        MenuOption("0.75", "0.75x", currentSpeed == 0.75f),
        MenuOption("1.0", "1x (Normal)", currentSpeed == 1.0f),
        MenuOption("1.25", "1.25x", currentSpeed == 1.25f),
        MenuOption("1.5", "1.5x", currentSpeed == 1.5f),
        MenuOption("2.0", "2x", currentSpeed == 2.0f)
    )

    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        speeds.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = { onSpeedSelected(it.toFloatOrNull() ?: 1.0f) }
            )
        }
    }
}

@Composable
fun QualityMenu(
    visible: Boolean,
    currentQuality: String,
    qualities: List<String>,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = qualities.map { q ->
        MenuOption(q, q, currentQuality == q)
    }

    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        options.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = onQualitySelected
            )
        }
    }
}

@Composable
fun SubtitleMenu(
    visible: Boolean,
    tracks: List<MenuOption>,
    onTrackSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        tracks.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = onTrackSelected
            )
        }
    }
}

@Composable
fun AudioMenu(
    visible: Boolean,
    tracks: List<MenuOption>,
    onTrackSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        tracks.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = onTrackSelected
            )
        }
    }
}
