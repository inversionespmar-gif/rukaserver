package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.rukatv.iptv.ui.theme.PlayerAccent

/**
 * Main playback controls bar: prev, rewind, play/pause, forward, next
 */
@Composable
fun PlayerControls(
    player: Player,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerControlButton(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Anterior",
            onClick = onPrev
        )

        PlayerControlButton(
            icon = Icons.Filled.Replay10,
            contentDescription = "Retroceder 10s",
            onClick = {
                val pos = runCatching { player.currentPosition }.getOrDefault(0L)
                player.seekTo((pos - 10000).coerceAtLeast(0L))
            }
        )

        val isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(PlayerAccent)
                .dpadFocus()
                .clickable { player.playWhenReady = !player.playWhenReady },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color(0xFF041E19),
                modifier = Modifier.size(28.dp)
            )
        }

        PlayerControlButton(
            icon = Icons.Filled.Forward10,
            contentDescription = "Adelantar 10s",
            onClick = {
                val pos = runCatching { player.currentPosition }.getOrDefault(0L)
                player.seekTo(pos + 10000)
            }
        )

        PlayerControlButton(
            icon = Icons.Filled.SkipNext,
            contentDescription = "Siguiente",
            onClick = onNext
        )
    }
}

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
