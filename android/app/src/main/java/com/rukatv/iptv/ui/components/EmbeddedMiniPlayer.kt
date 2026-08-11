package com.rukatv.iptv.ui.components

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.rukatv.iptv.ui.theme.Accent
import kotlinx.coroutines.delay

/**
 * Embedded mini video player that auto-plays a URL and exposes current position.
 * Clicking or pressing DPAD_CENTER triggers onFullScreen with current playback position.
 */
@Composable
fun EmbeddedMiniPlayer(
    url: String,
    startPositionMs: Long = 0L,
    modifier: Modifier = Modifier,
    onFullScreen: (positionMs: Long) -> Unit,
    onPositionUpdate: (positionMs: Long) -> Unit = {}
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    // Create and remember the ExoPlayer
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            volume = 0f // muted in mini player
        }
    }

    // Seek to saved position once ready
    var didSeek by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(startPositionMs) }

    DisposableEffect(url) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !didSeek && startPositionMs > 5000) {
                    player.seekTo(startPositionMs)
                    didSeek = true
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Poll position every 2s for "continuar viendo" updates
    LaunchedEffect(url) {
        while (true) {
            delay(2000)
            val pos = runCatching { player.currentPosition }.getOrDefault(0L)
            if (pos > 2000) {
                currentPositionMs = pos
                onPositionUpdate(pos)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Accent else Color(0x44FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interaction)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    onFullScreen(runCatching { player.currentPosition }.getOrDefault(currentPositionMs))
                    true
                } else false
            }
            .clickable(interactionSource = interaction, indication = null) {
                onFullScreen(runCatching { player.currentPosition }.getOrDefault(currentPositionMs))
            }
    ) {
        AndroidView(
            factory = { ctx: Context ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view -> view.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // Fullscreen hint icon (top-right corner)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color(0x88000000))
                .size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Fullscreen,
                contentDescription = "Pantalla completa",
                tint = if (focused) Accent else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
