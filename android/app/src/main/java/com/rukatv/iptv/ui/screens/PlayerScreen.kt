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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.player.TvPlayer
import com.rukatv.iptv.ui.components.player.PlayerOverlay
import com.rukatv.iptv.ui.components.player.PlayerControls
import com.rukatv.iptv.ui.components.player.PlayerProgressBar
import com.rukatv.iptv.ui.components.player.ScreenshotButton
import com.rukatv.iptv.ui.components.player.FavoriteButton
import com.rukatv.iptv.ui.components.player.PipButton
import com.rukatv.iptv.ui.components.player.SleepTimerButton
import com.rukatv.iptv.ui.components.player.SubtitleButton
import com.rukatv.iptv.ui.components.player.SpeedButton
import com.rukatv.iptv.ui.components.player.QualityButton
import com.rukatv.iptv.ui.theme.Accent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    var hasPlaybackError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Keep latest values accessible from the player listener without re-registering.
    val currentIndex by rememberUpdatedState(index)
    val currentQueue by rememberUpdatedState(queue)
    val currentIsSeries by rememberUpdatedState(isSeries)

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTimer by remember { mutableStateOf(0L) }

    var resumeOverlayVisible by remember { mutableStateOf(false) }
    var resumePosition by remember { mutableStateOf(0L) }
    var resumeTimestampText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val playerFocusRequester = remember { FocusRequester() }
    val continueFocusRequester = remember { FocusRequester() }
    val playNextFocusRequester = remember { FocusRequester() }
    val skipFocusRequester = remember { FocusRequester() }
    val errorFocusRequester = remember { FocusRequester() }

    fun nextEpisode() {
        if (index < queue.lastIndex) {
            index += 1
            showNextPrompt = false
            hasPlaybackError = false
        }
    }

    fun prevEpisode() {
        if (index > 0) {
            index -= 1
            showNextPrompt = false
            hasPlaybackError = false
        }
    }

    fun showControls() {
        controlsVisible = true
        controlsTimer = System.currentTimeMillis()
    }

    fun hideControlsDelayed() {
        scope.launch {
            delay(3000)
            if (System.currentTimeMillis() - controlsTimer >= 2900) {
                controlsVisible = false
            }
        }
    }

    fun safeSaveProgress() {
        val playItem = queue.getOrNull(index) ?: return
        val currentPos = runCatching { player.player.currentPosition }.getOrDefault(0L)
        val durationMs = runCatching { player.player.duration }.getOrDefault(0L).coerceAtLeast(0L)
        if (currentPos > 2000) {
            scope.launch {
                runCatching {
                    progressStore.saveProgress(
                        url = playItem.url,
                        positionMs = currentPos,
                        streamId = playItem.streamId,
                        title = playItem.title,
                        poster = playItem.poster,
                        durationMs = durationMs,
                        isSeries = isSeries
                    )
                }
            }
        }
    }


    // Force landscape for immersive playback + keep screen awake + release player on exit.
    DisposableEffect(Unit) {
        val prevOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            safeSaveProgress()
            player.release()
            activity?.requestedOrientation = prevOrientation
        }
    }

    BackHandler { onExit() }

    // Loads the active item into the player.
    LaunchedEffect(index) {
        showNextPrompt = false
        countdown = 10
        hasPlaybackError = false
        errorMessage = null

        // Check for saved progress
        val playUrl = queue.getOrNull(index)?.url
        if (playUrl != null) {
            val savedPos = runCatching { progressStore.getProgress(playUrl).first() }.getOrNull()
            if (savedPos != null && savedPos > 5000) {
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
            delay(10000)
            showSkipIntro = false
        }
    }

    // Detect end of media & errors -> autoplay next (Netflix style) or error recovery.
    // IMPORTANT: LaunchedEffect(Unit) runs once. We use rememberUpdatedState refs to always
    // read the current index/queue/isSeries without re-registering the listener.
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val q = currentQueue
                    val idx = currentIndex
                    if (idx <= q.lastIndex) {
                        scope.launch { runCatching { progressStore.removeProgress(q[idx].url) } }
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
                    safeSaveProgress()
                }
            }
        }
        // Only show the error UI when TvPlayer has exhausted all retries (fatal error)
        player.onFatalError = { error ->
            hasPlaybackError = true
            errorMessage = "Error de transmisión (${error.errorCodeName}). Revisa tu conexión a internet o intenta nuevamente."
        }
        player.setListener(listener)
        onDispose {
            runCatching { player.player.removeListener(listener) }
            player.onFatalError = null
        }
    }

    // Countdown for the "next episode" prompt.
    LaunchedEffect(showNextPrompt) {
        if (showNextPrompt) {
            countdown = 10
            for (i in 10 downTo 1) {
                countdown = i
                delay(1000)
            }
            if (showNextPrompt) {
                index += 1
                showNextPrompt = false
            }
        }
    }

    // Save progress periodically while playing
    LaunchedEffect(index) {
        while (true) {
            delay(8000)
            val isPlaying = runCatching { player.player.isPlaying }.getOrDefault(false)
            if (isPlaying) {
                safeSaveProgress()
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

    LaunchedEffect(Unit) {
        runCatching { playerFocusRequester.requestFocus() }
    }

    val controlsVisibleRef = rememberUpdatedState(controlsVisible)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerFocusRequester)
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown) {
                    val nativeEvent = ev.nativeKeyEvent
                    val keyCode = nativeEvent.keyCode
                    showControls()
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_BUTTON_A,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            runCatching { player.player.playWhenReady = !player.player.playWhenReady }
                            hideControlsDelayed()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            runCatching { player.player.playWhenReady = true }
                            hideControlsDelayed()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            runCatching { player.player.playWhenReady = false }
                            hideControlsDelayed()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_REWIND,
                        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                            val pos = runCatching { player.player.currentPosition }.getOrDefault(0L)
                            player.seekTo((pos - 10000).coerceAtLeast(0L))
                            hideControlsDelayed()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                            val pos = runCatching { player.player.currentPosition }.getOrDefault(0L)
                            player.seekTo(pos + 10000)
                            hideControlsDelayed()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            nextEpisode()
                            hideControlsDelayed()
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            prevEpisode()
                            hideControlsDelayed()
                            true
                        }
                        else -> {
                            hideControlsDelayed()
                            false
                        }
                    }
                } else false
            }
    ) {
        // Full-screen video View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { fl ->
                    fl.keepScreenOn = true
                    val pv = player.playerView(ctx) { visible ->
                        controlsVisible = visible
                    }
                    pv.setOnKeyListener { _, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        val ctrlVisible = controlsVisibleRef.value
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (!ctrlVisible) {
                                    runCatching { player.player.playWhenReady = !player.player.playWhenReady }
                                    showControls()
                                    hideControlsDelayed()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (!ctrlVisible) {
                                    val pos = runCatching { player.player.currentPosition }.getOrDefault(0L)
                                    player.seekTo((pos - 10000).coerceAtLeast(0L))
                                    showControls()
                                    hideControlsDelayed()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (!ctrlVisible) {
                                    val pos = runCatching { player.player.currentPosition }.getOrDefault(0L)
                                    player.seekTo(pos + 10000)
                                    showControls()
                                    hideControlsDelayed()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                runCatching { player.player.playWhenReady = !player.player.playWhenReady }
                                showControls()
                                hideControlsDelayed()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                nextEpisode()
                                showControls()
                                hideControlsDelayed()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                prevEpisode()
                                showControls()
                                hideControlsDelayed()
                                true
                            }
                            else -> {
                                showControls()
                                hideControlsDelayed()
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

        // Premium Player Overlay
        PlayerOverlay(
            title = queue.getOrNull(index)?.title ?: "",
            visible = controlsVisible && !hasPlaybackError,
            topActions = {
                ScreenshotButton(onScreenshot = { /* Captura de pantalla - TODO */ })
                FavoriteButton(
                    isFavorite = false,
                    onToggle = { /* Favorito - TODO */ }
                )
            },
            bottomContent = {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerProgressBar(
                        currentPositionMs = runCatching { player.player.currentPosition }.getOrDefault(0L),
                        durationMs = runCatching { player.player.duration }.getOrDefault(0L),
                        onSeek = { player.seekTo(it) }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerControls(
                            player = player.player,
                            onPrev = { prevEpisode() },
                            onNext = { nextEpisode() },
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SubtitleButton(onClick = { /* Menú de subtítulos - TODO */ })
                            SpeedButton(currentSpeed = 1.0f, onClick = { /* Menú de velocidad - TODO */ })
                            QualityButton(onClick = { /* Menú de calidad - TODO */ })
                            PipButton(onPipRequested = { /* Picture-in-Picture - TODO */ })
                            SleepTimerButton(remainingMinutes = null, onSelectTimer = { /* Temporizador - TODO */ })
                        }
                    }
                }
            }
        )

        // Error Screen Overlay (If stream fails/disconnects)
        if (hasPlaybackError) {
            val errorInteraction = remember { MutableInteractionSource() }
            val errorFocused = errorInteraction.collectIsFocusedAsState().value

            LaunchedEffect(hasPlaybackError) {
                errorFocusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Error de reproducción",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "Error al reproducir contenido",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage ?: "No se pudo cargar la señal de video.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier.width(360.dp),
                        lineHeight = 20.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Retry button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (errorFocused) Color.White else Accent)
                                .border(if (errorFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                                .focusRequester(errorFocusRequester)
                                .focusable(interactionSource = errorInteraction)
                                .clickable(interactionSource = errorInteraction, indication = null) {
                                    hasPlaybackError = false
                                    if (queue.isNotEmpty()) {
                                        player.prepare(queue[index.coerceIn(0, queue.lastIndex)].url)
                                    }
                                }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reintentar",
                                    tint = Color(0xFF041E19),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Reintentar", color = Color(0xFF041E19), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Exit button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { onExit() }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Volver", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // "Saltar intro" — Netflix style: bottom-end pill, auto-hides.
        AnimatedVisibility(
            visible = isSeries && showSkipIntro && !hasPlaybackError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            val skipInteraction = remember { MutableInteractionSource() }
            val skipFocused = skipInteraction.collectIsFocusedAsState().value

            LaunchedEffect(showSkipIntro) {
                if (showSkipIntro) runCatching { skipFocusRequester.requestFocus() }
            }

            Box(
                modifier = Modifier
                    .padding(bottom = 80.dp, end = 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (skipFocused) Accent else Color(0xCC000000))
                    .border(
                        if (skipFocused) 2.dp else 1.dp,
                        if (skipFocused) Accent else Color.White.copy(alpha = 0.55f),
                        RoundedCornerShape(4.dp)
                    )
                    .focusRequester(skipFocusRequester)
                    .focusable(interactionSource = skipInteraction)
                    .clickable(interactionSource = skipInteraction, indication = null) {
                        player.seekTo((SKIP_INTRO_SECONDS * 1000).toLong())
                        showSkipIntro = false
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Saltar intro",
                    color = if (skipFocused) Color(0xFF06231F) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Resume playback overlay
        AnimatedVisibility(
            visible = resumeOverlayVisible && !hasPlaybackError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val contInteraction = remember { MutableInteractionSource() }
            val contFocused = contInteraction.collectIsFocusedAsState().value
            val restartInteraction = remember { MutableInteractionSource() }
            val restartFocused = restartInteraction.collectIsFocusedAsState().value

            LaunchedEffect(resumeOverlayVisible) {
                if (resumeOverlayVisible) runCatching { continueFocusRequester.requestFocus() }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6121212))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "¿Continuar viendo?",
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
                                .background(if (contFocused) Color.White else Accent)
                                .border(if (contFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                                .focusRequester(continueFocusRequester)
                                .focusable(interactionSource = contInteraction)
                                .clickable(interactionSource = contInteraction, indication = null) {
                                    player.seekTo(resumePosition)
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
                                .background(if (restartFocused) Accent.copy(alpha = 0.2f) else Color.Transparent)
                                .border(if (restartFocused) 2.dp else 1.dp, if (restartFocused) Accent else Color(0xFF444444), RoundedCornerShape(6.dp))
                                .focusable(interactionSource = restartInteraction)
                                .clickable(interactionSource = restartInteraction, indication = null) {
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
                                color = if (restartFocused) Accent else Color(0xFFCCCCCC),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Next episode prompt — Netflix panel at bottom-right
        AnimatedVisibility(
            visible = showNextPrompt && index < queue.lastIndex && !hasPlaybackError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            val playNextInteraction = remember { MutableInteractionSource() }
            val playNextFocused = playNextInteraction.collectIsFocusedAsState().value
            val cancelNextInteraction = remember { MutableInteractionSource() }
            val cancelNextFocused = cancelNextInteraction.collectIsFocusedAsState().value

            LaunchedEffect(showNextPrompt) {
                if (showNextPrompt) runCatching { playNextFocusRequester.requestFocus() }
            }

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
                                .background(if (playNextFocused) Color.White else Accent)
                                .border(if (playNextFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(5.dp))
                                .focusRequester(playNextFocusRequester)
                                .focusable(interactionSource = playNextInteraction)
                                .clickable(interactionSource = playNextInteraction, indication = null) { index += 1; showNextPrompt = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = Color(0xFF06231F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Reproducir",
                                    color = Color(0xFF06231F),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Cancel
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (cancelNextFocused) Accent.copy(alpha = 0.2f) else Color.Transparent)
                                .border(if (cancelNextFocused) 2.dp else 1.dp, if (cancelNextFocused) Accent else Color(0xFF444444), RoundedCornerShape(5.dp))
                                .focusable(interactionSource = cancelNextInteraction)
                                .clickable(interactionSource = cancelNextInteraction, indication = null) { showNextPrompt = false }
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancelar", color = if (cancelNextFocused) Accent else Color(0xFFCCCCCC), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
