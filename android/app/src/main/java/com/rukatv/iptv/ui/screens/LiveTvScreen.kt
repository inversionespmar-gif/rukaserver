package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.player.DrmPlayer
import com.rukatv.iptv.player.VlcPlayer
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.Chip
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.IptvChannelOverlay
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.LiveTvViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveTvScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String) -> Unit,
    onFullscreen: (Boolean) -> Unit = {}
) {
    val vm = remember { LiveTvViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    val channels = vm.filteredChannels()
    var query by remember { mutableStateOf("") }
    val filtered = remember(channels, query) {
        if (query.isBlank()) channels else channels.filter { it.name.contains(query, true) }
    }

    var selectedIndex by remember { mutableStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }

    // IPTV Loading Overlay States
    var isChannelLoading by remember { mutableStateOf(true) }
    var showChannelOverlay by remember { mutableStateOf(true) }

    LaunchedEffect(fullscreen) { onFullscreen(fullscreen) }

    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()

    val vlcPlayer = remember { VlcPlayer(context) }
    val drmPlayer = remember { DrmPlayer(context) }
    var useDrm by remember { mutableStateOf(false) }
    val currentFiltered by rememberUpdatedState(filtered)

    fun isDrmUrl(url: String): Boolean {
        return url.contains(".mpd") ||
            url.contains("embed.php") ||
            url.contains("telelibrefull") ||
            url.contains("bestleague") ||
            url.contains("tok.html")
    }

    fun resolveUrl(url: String): String {
        if (url.startsWith("/proxy/") || url.startsWith("/live/") || url.startsWith("/movie/") || url.startsWith("/series/")) {
            val host = catalog.creds.host.trimEnd('/')
            return "$host$url"
        }
        return url
    }

    fun safePlayUrl(url: String) {
        try {
            isChannelLoading = true
            showChannelOverlay = true
            val resolved = resolveUrl(url)
            val isDrm = isDrmUrl(resolved)
            if (isDrm != useDrm) {
                if (useDrm) drmPlayer.stop() else vlcPlayer.stop()
                useDrm = isDrm
            }
            if (isDrm) {
                drmPlayer.stop()
                drmPlayer.play(resolved)
            } else {
                vlcPlayer.stop()
                vlcPlayer.play(resolved)
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveTv", "play error: ${e.message}")
        }
    }

    // Auto-dismiss IPTV loading bar after video stream connects
    LaunchedEffect(selectedIndex, useDrm) {
        delay(1800)
        isChannelLoading = false
        delay(2500)
        showChannelOverlay = false
    }

    LaunchedEffect(Unit) {
        val list = currentFiltered
        if (list.isNotEmpty()) {
            safePlayUrl(list[0].streamUrl)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { vlcPlayer.stop() } catch (_: Exception) {}
            try { vlcPlayer.release() } catch (_: Exception) {}
            try { drmPlayer.stop() } catch (_: Exception) {}
            try { drmPlayer.release() } catch (_: Exception) {}
        }
    }
    val listState = rememberLazyListState()
    val overlayListState = rememberLazyListState()

    fun playIndex(i: Int) {
        val list = currentFiltered
        if (i < 0 || i >= list.size) return
        selectedIndex = i
        if (list.isNotEmpty()) {
            safePlayUrl(list[i].streamUrl)
        }
    }

    var numberBuffer by remember { mutableStateOf("") }
    LaunchedEffect(numberBuffer) {
        if (numberBuffer.isNotEmpty()) {
            delay(700)
            val n = numberBuffer.toIntOrNull()
            val list = currentFiltered
            if (n != null && n in 1..list.size) {
                val idx = n - 1
                playIndex(idx)
                listState.scrollToItem(idx)
            }
            numberBuffer = ""
        }
    }

    BackHandler(enabled = fullscreen) {
        if (overlay) overlay = false else fullscreen = false
    }

    LaunchedEffect(overlay) {
        if (overlay && filtered.isNotEmpty()) {
            overlayListState.scrollToItem(selectedIndex.coerceIn(0, filtered.lastIndex))
        }
    }

    val currentChannel = filtered.getOrNull(selectedIndex)

    Box(Modifier.fillMaxSize().background(Color.Black).onKeyEvent { ev ->
        if (ev.type == KeyEventType.KeyDown) {
            when (ev.key) {
                Key.Zero -> { numberBuffer += "0"; true }
                Key.One -> { numberBuffer += "1"; true }
                Key.Two -> { numberBuffer += "2"; true }
                Key.Three -> { numberBuffer += "3"; true }
                Key.Four -> { numberBuffer += "4"; true }
                Key.Five -> { numberBuffer += "5"; true }
                Key.Six -> { numberBuffer += "6"; true }
                Key.Seven -> { numberBuffer += "7"; true }
                Key.Eight -> { numberBuffer += "8"; true }
                Key.Nine -> { numberBuffer += "9"; true }
                Key.DirectionUp -> {
                    if (fullscreen && !overlay) {
                        playIndex((selectedIndex - 1 + filtered.size) % filtered.size)
                        true
                    } else false
                }
                Key.DirectionDown -> {
                    if (fullscreen && !overlay) {
                        playIndex((selectedIndex + 1) % filtered.size)
                        true
                    } else false
                }
                Key.DirectionCenter, Key.Enter -> {
                    if (fullscreen && !overlay) {
                        showChannelOverlay = true
                        overlay = true
                        true
                    } else false
                }
                else -> false
            }
        } else false
    }) {
        // Player surface - always present
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val currentView = if (useDrm) drmPlayer.view else vlcPlayer.view
                (currentView.parent as? ViewGroup)?.removeView(currentView)
                android.widget.FrameLayout(ctx).also { fl ->
                    fl.keepScreenOn = true
                    fl.addView(
                        currentView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            },
            update = { view ->
                val currentView = if (useDrm) drmPlayer.view else vlcPlayer.view
                if (currentView.parent != view) {
                    (currentView.parent as? ViewGroup)?.removeView(currentView)
                    (view as android.widget.FrameLayout).addView(
                        currentView,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }
        )

        // IPTV Channel Loading & OSD Overlay
        if (currentChannel != null) {
            IptvChannelOverlay(
                visible = showChannelOverlay || isChannelLoading,
                channelNumber = "%03d".format(selectedIndex + 1),
                channelName = currentChannel.name,
                channelLogo = currentChannel.streamIcon,
                categoryName = state.categories.find { it.first == state.selectedCategory }?.second ?: "En Vivo",
                isLoading = isChannelLoading
            )
        }

        if (fullscreen) {
            // ── Fullscreen mode ──────────────────────────────────────────────
            Box(Modifier.fillMaxSize().clickable {
                showChannelOverlay = true
                overlay = !overlay
            })

            if (overlay) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000))
                        .clickable { overlay = false }
                )
                LazyColumn(
                    state = overlayListState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(400.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filtered) { i, ch ->
                        val itemFocusRequester = remember { FocusRequester() }
                        LaunchedEffect(overlay) {
                            if (overlay && i == selectedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))) {
                                itemFocusRequester.requestFocus()
                            }
                        }
                        ChannelRow(
                            index = i,
                            name = ch.name,
                            logo = ch.streamIcon,
                            onFocus = { playIndex(i) },
                            modifier = Modifier.focusRequester(itemFocusRequester),
                            onClick = { playIndex(i); overlay = false }
                        )
                    }
                }
            }
        } else {
            // ── Split view ──────────────────────────────────────────────────
            Row(Modifier.fillMaxSize()) {
                // ── Left: channel list (solid background covers video) ─────
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(0.38f)
                        .background(Background)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface)
                            .focusable()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(Accent),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("Filtrar canales...", color = Color(0xFF5A6375), fontSize = 15.sp)
                            }
                            inner()
                        }
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Chip("Todos", state.selectedCategory == null) { vm.selectCategory(null) }
                        }
                        items(state.categories) { (id, name) ->
                            Chip(name, state.selectedCategory == id) { vm.selectCategory(id) }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(filtered) { i, ch ->
                            ChannelRow(
                                index = i,
                                name = ch.name,
                                logo = ch.streamIcon,
                                onFocus = { playIndex(i) },
                                onClick = { playIndex(i) }
                            )
                        }
                    }
                }

                // ── Right: overlays on top of video ──────────────────────────
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(0.62f)
                        .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = Accent.copy(alpha = 0.08f),
                                spotColor = Color.Black
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .focusable()
                            .clickable { fullscreen = true }
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable { fullscreen = true }
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.35f)
                                .align(Alignment.TopStart)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x99000000), Color.Transparent)
                                    )
                                )
                        )
                        Text(
                            text = filtered.getOrNull(selectedIndex)?.name ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                        val favKey = "live:${filtered.getOrNull(selectedIndex)?.streamId}"
                        val isFav = favSet.contains(favKey)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .border(
                                    1.dp,
                                    if (isFav) Accent else Color(0xFF555555),
                                    CircleShape
                                )
                                .clickable { scope.launch { favorites.toggle(favKey) } }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFav) "Quitar favorito" else "Agregar favorito",
                                tint = if (isFav) Accent else Color(0xFFAAAAAA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Pantalla completa",
                            tint = Color(0x88FFFFFF),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
