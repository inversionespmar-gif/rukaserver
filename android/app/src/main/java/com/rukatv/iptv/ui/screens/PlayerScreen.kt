package com.rukatv.iptv.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.player.TvPlayer
import com.rukatv.iptv.ui.theme.Accent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// Seconds to skip past the intro when playing a series episode.
const val SKIP_INTRO_SECONDS = 90

@Composable
fun PlayerScreen(
    queue: List<PlayItem>,
    startIndex: Int = 0,
    isSeries: Boolean = false,
    progressStore: PlaybackProgressStore,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val player = remember { TvPlayer(context) }
    var index by remember { mutableStateOf(startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))) }
    var showNextPrompt by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(10) }
    var showSkipIntro by remember { mutableStateOf(false) }

    // Keep latest values accessible from the player listener without re-registering.
    val currentIndex by rememberUpdatedState(index)
    val currentQueue by rememberUpdatedState(queue)
    val currentIsSeries by rememberUpdatedState(isSeries)

    var controlsVisible by remember { mutableStateOf(true) }

    var resumeOverlayVisible by remember { mutableStateOf(false) }
    var resumePosition by remember { mutableStateOf(0L) }
    var resumeTimestampText by remember { mutableStateOf("") }

    fun nextEpisode() {
        if (index < queue.lastIndex) {
            index += 1
            showNextPrompt = false
        }
    }

    fun prevEpisode() {
        if (index > 0) {
            index -= 1
            showNextPrompt = false
        }
    }

    // Force landscape for immersive playback + release player on exit.
    DisposableEffect(Unit) {
        val prevOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            runBlocking {
                val url = queue.getOrNull(index)?.url ?: return@runBlocking
                progressStore.saveProgress(url, player.player.currentPosition)
            }
            player.release()
            activity?.requestedOrientation = prevOrientation
        }
    }

    BackHandler { onExit() }

    // Loads the active item into the player.
    LaunchedEffect(index) {
        showNextPrompt = false
        countdown = 10

        // Check for saved progress
        val playUrl = queue.getOrNull(index)?.url
        if (playUrl != null) {
            val savedPos = progressStore.getProgress(playUrl).first()
            if (savedPos != null && savedPos > 0) {
                resumePosition = savedPos
                val totalSec = savedPos / 1000
                resumeTimestampText = "%d:%02d".format(totalSec / 60, totalSec % 60)
                resumeOverlayVisible = true
            }
        }

        if (queue.isNotEmpty()) {
            player.prepare(queue[index.coerceIn(0, queue.lastIndex)].url)
        }
        // Show "Skip intro" for the first 10s of a series episode (Netflix style).
        if (isSeries) {
            showSkipIntro = true
            kotlinx.coroutines.delay(10000)
            showSkipIntro = false
        }
    }

    // Detect end of media -> autoplay next (Netflix style) for series.
    LaunchedEffect(Unit) {
        player.setListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val q = currentQueue
                    val idx = currentIndex
                    if (idx <= q.lastIndex) {
                        scope.launch { progressStore.removeProgress(q[idx].url) }
                    }
                    if (currentIsSeries && idx < q.lastIndex) {
                        showNextPrompt = true
                    } else {
                        onExit()
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    val url = currentQueue.getOrNull(currentIndex)?.url ?: return
                    scope.launch {
                        progressStore.saveProgress(url, player.player.currentPosition)
                    }
                }
            }
        })
    }

    // Countdown for the "next episode" prompt.
    LaunchedEffect(showNextPrompt) {
        if (showNextPrompt) {
            countdown = 10
            for (i in 10 downTo 1) {
                countdown = i
                kotlinx.coroutines.delay(1000)
            }
            if (showNextPrompt) {
                index += 1
                showNextPrompt = false
            }
        }
    }

    // Save progress every 5 seconds while playing
    LaunchedEffect(index) {
        while (true) {
            delay(5000)
            val url = queue.getOrNull(index)?.url ?: continue
            if (player.player.isPlaying) {
                progressStore.saveProgress(url, player.player.currentPosition)
            }
        }
    }

    // Auto-dismiss resume overlay after 5s
    LaunchedEffect(resumeOverlayVisible) {
        if (resumeOverlayVisible) {
            delay(5000)
            resumeOverlayVisible = false
        }
    }

    val controlsVisibleRef = rememberUpdatedState(controlsVisible)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Full-screen video
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { fl ->
                    val pv = player.playerView(ctx) { visible ->
                        controlsVisible = visible
                    }
                    pv.setOnKeyListener { _, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        val ctrlVisible = controlsVisibleRef.value
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (!ctrlVisible) {
                                    player.player.playWhenReady = !player.player.playWhenReady
                                    pv.showController()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (!ctrlVisible) {
                                    val newPos = (player.player.currentPosition - 10000).coerceAtLeast(0L)
                                    player.player.seekTo(newPos)
                                    pv.showController()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (!ctrlVisible) {
                                    val newPos = player.player.currentPosition + 10000
                                    player.player.seekTo(newPos)
                                    pv.showController()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                player.player.playWhenReady = !player.player.playWhenReady
                                pv.showController()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                nextEpisode()
                                pv.showController()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                prevEpisode()
                                pv.showController()
                                true
                            }
                            else -> {
                                pv.showController()
                                false
                            }
                        }
                    }
                    fl.addView(
                        pv,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }
        )

        // Title and Top Gradient overlay hidden/shown with controller controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xBB000000), Color.Transparent)
                        )
                    )
            ) {
                Text(
                    text = queue.getOrNull(index)?.title ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        // "Saltar intro" — Netflix style: bottom-end pill, auto-hides.
        AnimatedVisibility(
            visible = isSeries && showSkipIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 80.dp, end = 24.dp) // above player controls
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.55f),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        player.seekTo((SKIP_INTRO_SECONDS * 1000).toLong())
                        showSkipIntro = false
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Saltar intro",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Resume playback overlay
        AnimatedVisibility(
            visible = resumeOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6121212))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Continuar viendo?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        resumeTimestampText,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Accent)
                                .clickable {
                                    player.player.seekTo(resumePosition)
                                    resumeOverlayVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Continuar",
                                color = Color(0xFF06231F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                                .clickable {
                                    scope.launch {
                                        val url = queue.getOrNull(index)?.url ?: return@launch
                                        progressStore.removeProgress(url)
                                    }
                                    resumeOverlayVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Empezar de nuevo",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Next episode prompt — Netflix panel at bottom-right
        AnimatedVisibility(
            visible = showNextPrompt && index < queue.lastIndex,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .width(290.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xEA121212))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Siguiente episodio",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        queue.getOrNull(index + 1)?.title ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        lineHeight = 20.sp
                    )
                    // Countdown progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF2A2A2A))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(countdown / 10f)
                                .height(3.dp)
                                .background(Accent)
                        )
                    }
                    Text(
                        "Reproduciendo en ${countdown}s",
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Play now
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Accent)
                                .clickable { index += 1; showNextPrompt = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "▶  Reproducir",
                                color = Color(0xFF06231F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Cancel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(5.dp))
                                .clickable { showNextPrompt = false }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancelar", color = Color(0xFFCCCCCC), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
