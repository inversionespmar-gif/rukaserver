package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.PlayerAccent

@Composable
fun SubtitleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CC",
            color = if (isActive) PlayerAccent else Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SpeedButton(
    currentSpeed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val speedText = if (currentSpeed == 1.0f) "1x" else "${currentSpeed}x"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = speedText,
            color = if (currentSpeed != 1.0f) PlayerAccent else Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun QualityButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quality: String = "HD"
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = quality,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
