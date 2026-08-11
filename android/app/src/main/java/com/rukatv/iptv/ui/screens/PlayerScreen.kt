package com.rukatv.iptv.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.core.view.drawToBitmap
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.player.SubtitleTrackInfo
import com.rukatv.iptv.player.TvPlayer
import com.rukatv.iptv.player.VideoQualityInfo
import com.rukatv.iptv.ui.components.player.FavoriteButton
import com.rukatv.iptv.ui.components.player.MenuOption
import com.rukatv.iptv.ui.components.player.PipButton
import com.rukatv.iptv.ui.components.player.PlayerControls
import com.rukatv.iptv.ui.components.player.PlayerOverlay
import com.rukatv.iptv.ui.components.player.PlayerProgressBar
import com.rukatv.iptv.ui.components.player.QualityButton
import com.rukatv.iptv.ui.components.player.QualityMenu
import com.rukatv.iptv.ui.components.player.ScreenshotButton
import com.rukatv.iptv.ui.components.player.SleepTimerButton
import com.rukatv.iptv.ui.components.player.SpeedButton
import com.rukatv.iptv.ui.components.player.SpeedMenu
import com.rukatv.iptv.ui.components.player.SubtitleButton
import com.rukatv.iptv.ui.components.player.SubtitleMenu
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
    startPositionMs: Long = 0L,
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

    // Reactive playback states for UI synchronization
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    var availableSubtitles by remember { mutableStateOf<List<SubtitleTrackInfo>>(emptyList()) }
    var selectedSubtitleId by remember { mutableStateOf<String?>(null) }

    var availableQualities by remember { mutableStateOf<List<VideoQualityInfo>>(emptyList()) }
    var selectedQualityLabel by remember { mutableStateOf("HD") }

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Keep latest values accessible from the player listener without re-registering.
    val currentIndex by rememberUpdatedState(index)
    val currentQueue by rememberUpdatedState(queue)
    val currentIsSeries by rememberUpdatedState(isSeries)

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsTimer by remember { mutableStateOf(0L) }

    // Premium player features state
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFavorite by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableStateOf<Int?>(null) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }

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
        val duration = runCatching { player.player.duration }.getOrDefault(0L).coerceAtLeast(0L)
        if (currentPos > 2000) {
            scope.launch {
                runCatching {
                    progressStore.saveProgress(
                        url = playItem.url,
                        positionMs = currentPos,
                        streamId = playItem.streamId,
                        title = playItem.title,
                        poster = playItem.poster,
                        durationMs = duration,
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

        if (queue.isNotEmpty()) {
            player.prepare(queue[index.coerceIn(0, queue.lastIndex)].url)
        }

        // If startPositionMs is provided (from mini player), seek directly — no resume overlay.
        if (index == startIndex && startPositionMs > 5000) {
            delay(800) // wait for player to buffer before seeking
            runCatching { player.player.seekTo(startPositionMs) }
        } else {
            // Check for saved progress and show resume overlay
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
        }

        // Show "Skip intro" for the first 10s of a series episode (Netflix style).
        if (isSeries) {
            showSkipIntro = true
            delay(10000)
            showSkipIntro = false
        }
    }

    // Register Player Listener to observe play/pause, time, state, tracks in real-time
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    durationMs = runCatching { player.player.duration }.getOrDefault(0L).coerceAtLeast(0L)
                    availableSubtitles = player.getSubtitleTracks()
                    availableQualities = player.getVideoQualities()
                }
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

            override fun onTracksChanged(tracks: Tracks) {
                availableSubtitles = player.getSubtitleTracks()
                availableQualities = player.getVideoQualities()
                selectedSubtitleId = availableSubtitles.find { it.isSelected }?.id
                val activeQuality = availableQualities.find { it.isSelected }
                selectedQualityLabel = activeQuality?.label ?: "HD"
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    safeSaveProgress()
                }
            }
        }
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

    // Continuous ticker loop to update current playback position in real-time
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = runCatching { player.player.currentPosition }.getOrDefault(0L).coerceAtLeast(0L)
            val dur = runCatching { player.player.duration }.getOrDefault(0L)
            if (dur > 0) durationMs = dur
            delay(500)
        }
    }

    // Sleep Timer auto-dismiss / exit handler
    LaunchedEffect(sleepTimerMinutes) {
        val mins = sleepTimerMinutes ?: return@LaunchedEffect
        if (mins > 0) {
            delay(mins * 60 * 1000L)
            runCatching { player.player.pause() }
            Toast.makeText(context, "Temporizador de apagado finalizado", Toast.LENGTH_LONG).show()
            onExit()
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
                    playerViewRef = pv

                    // Touch listener to show/hide controls on tap
                    fl.setOnTouchListener { _, _ ->
                        if (controlsVisibleRef.value) {
                            controlsVisible = false
                        } else {
                            showControls()
                            hideControlsDelayed()
                        }
                        true
                    }

                    pv.setOnKeyListener { _, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        val ctrlVisible = controlsVisibleRef.value
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (!ctrlVisible) {
                                    val nextState = !player.player.playWhenReady
                                    runCatching { player.player.playWhenReady = nextState }
                                    isPlaying = nextState
                                    showControls()
                                    hideControlsDelayed()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (!ctrlVisible) {
                                    val pos = runCatching { player.player.currentPosition }.getOrDefault(0L)
                                    player.seekTo((pos - 10000).coerceAtLeast(0L))
                                    currentPositionMs = (pos - 10000).coerceAtLeast(0L)
                                    showControls()
                                    hideControlsDelayed()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (!ctrlVisible) {
                                    val pos = runCatching { player.player.currentPosition }.getOrDefault(0L)
                                    player.seekTo(pos + 10000)
                                    currentPositionMs = pos + 10000
                                    showControls()
                                    hideControlsDelayed()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                val nextState = !player.player.playWhenReady
                                runCatching { player.player.playWhenReady = nextState }
                                isPlaying = nextState
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
            onTap = {
                if (controlsVisible) {
                    controlsVisible = false
                } else {
                    showControls()
                    hideControlsDelayed()
                }
            },
            topActions = {
                ScreenshotButton(onScreenshot = {
                    playerViewRef?.let { capturePlayerFrame(context, it) }
                    showControls()
                    hideControlsDelayed()
                })
                FavoriteButton(
                    isFavorite = isFavorite,
                    onToggle = {
                        isFavorite = !isFavorite
                        showControls()
                        hideControlsDelayed()
                    }
                )
            },
            bottomContent = {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerProgressBar(
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        onSeek = { pos ->
                            player.seekTo(pos)
                            currentPositionMs = pos
                        }
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
                            isPlaying = isPlaying,
                            onTogglePlayPause = {
                                val nextState = !player.player.playWhenReady
                                runCatching { player.player.playWhenReady = nextState }
                                isPlaying = nextState
                            },
                            onPrev = { prevEpisode() },
                            onNext = { nextEpisode() },
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SubtitleButton(
                                onClick = {
                                    showSubtitleMenu = !showSubtitleMenu
                                    showSpeedMenu = false
                                    showQualityMenu = false
                                    showControls()
                                },
                                isActive = selectedSubtitleId != null
                            )
                            SpeedButton(
                                currentSpeed = currentSpeed,
                                onClick = {
                                    showSpeedMenu = !showSpeedMenu
                                    showSubtitleMenu = false
                                    showQualityMenu = false
                                    showControls()
                                }
                            )
                            QualityButton(
                                onClick = {
                                    showQualityMenu = !showQualityMenu
                                    showSubtitleMenu = false
                                    showSpeedMenu = false
                                    showControls()
                                },
                                quality = selectedQualityLabel
                            )
                            PipButton(onPipRequested = {
                                triggerPip(context)
                                showControls()
                                hideControlsDelayed()
                            })
                            SleepTimerButton(
                                remainingMinutes = sleepTimerMinutes,
                                onSelectTimer = { minutes ->
                                    sleepTimerMinutes = minutes
                                    showControls()
                                    hideControlsDelayed()
                                }
                                )
                        }
                    }
                }
            }
        )

        // Subtitle, Quality & Speed Dropdown Menus
        val subtitleMenuOptions = remember(availableSubtitles, selectedSubtitleId) {
            val list = mutableListOf(
                MenuOption("OFF", "Desactivado", selectedSubtitleId == null)
            )
            availableSubtitles.forEach { sub ->
                list.add(MenuOption(sub.id, sub.label, sub.id == selectedSubtitleId))
            }
            list
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 24.dp)
        ) {
            SubtitleMenu(
                visible = showSubtitleMenu,
                tracks = subtitleMenuOptions,
                onTrackSelected = { selectedId ->
                    if (selectedId == "OFF") {
                        player.selectSubtitleTrack(null)
                        selectedSubtitleId = null
                    } else {
                        val track = availableSubtitles.find { it.id == selectedId }
                        player.selectSubtitleTrack(track)
                        selectedSubtitleId = selectedId
                    }
                    showSubtitleMenu = false
                    showControls()
                    hideControlsDelayed()
                },
                onDismiss = {
                    showSubtitleMenu = false
                    showControls()
                    hideControlsDelayed()
                }
            )

            QualityMenu(
                visible = showQualityMenu,
                currentQuality = selectedQualityLabel,
                qualities = listOf("AUTO") + availableQualities.map { it.label }.distinct(),
                onQualitySelected = { qLabel ->
                    player.selectVideoQuality(qLabel)
                    selectedQualityLabel = if (qLabel == "AUTO") "HD" else qLabel
                    showQualityMenu = false
                    showControls()
                    hideControlsDelayed()
                },
                onDismiss = {
                    showQualityMenu = false
                    showControls()
                    hideControlsDelayed()
                }
            )

            SpeedMenu(
                visible = showSpeedMenu,
                currentSpeed = currentSpeed,
                onSpeedSelected = { speed ->
                    currentSpeed = speed
                    runCatching { player.player.setPlaybackSpeed(speed) }
                    showSpeedMenu = false
                    showControls()
                    hideControlsDelayed()
                },
                onDismiss = {
                    showSpeedMenu = false
                    showControls()
                    hideControlsDelayed()
                }
            )
        }

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

private fun triggerPip(context: Context) {
    val activity = context as? Activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val hasPip = context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        if (hasPip) {
            runCatching {
                val params = PictureInPictureParams.Builder().build()
                activity.enterPictureInPictureMode(params)
            }.onFailure {
                Toast.makeText(context, "Error al activar modo PiP", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Modo PiP no soportado en este dispositivo", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Modo PiP requiere Android 8.0 o superior", Toast.LENGTH_SHORT).show()
    }
}

private fun capturePlayerFrame(context: Context, playerView: PlayerView) {
    val activity = context as? Activity ?: return
    val surfaceView = playerView.videoSurfaceView
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && surfaceView is SurfaceView && surfaceView.holder.surface.isValid) {
        val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
        val locationOfViewInWindow = IntArray(2)
        surfaceView.getLocationInWindow(locationOfViewInWindow)
        val rect = Rect(
            locationOfViewInWindow[0],
            locationOfViewInWindow[1],
            locationOfViewInWindow[0] + surfaceView.width,
            locationOfViewInWindow[1] + surfaceView.height
        )
        PixelCopy.request(surfaceView, rect, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                saveBitmapToGallery(context, bitmap)
            } else {
                activity.runOnUiThread {
                    Toast.makeText(context, "No se pudo realizar la captura de pantalla", Toast.LENGTH_SHORT).show()
                }
            }
        }, Handler(Looper.getMainLooper()))
    } else {
        runCatching {
            val bitmap = playerView.drawToBitmap()
            saveBitmapToGallery(context, bitmap)
        }.onFailure {
            Toast.makeText(context, "No se pudo guardar la captura de pantalla", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "RukaTv_${System.currentTimeMillis()}.jpg"
    val activity = context as? Activity
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RukaTV")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/RukaTV"
            val file = java.io.File(imagesDir)
            if (!file.exists()) file.mkdirs()
            val imageFile = java.io.File(imagesDir, filename)
            java.io.FileOutputStream(imageFile).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
        }
        activity?.runOnUiThread {
            Toast.makeText(context, "Captura de pantalla guardada en Imágenes", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        activity?.runOnUiThread {
            Toast.makeText(context, "Error al guardar captura: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

