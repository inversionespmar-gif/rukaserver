package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.player.TvPlayer
import com.rukatv.iptv.player.StreamKind
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.Chip
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.LiveTvViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Deduce el tipo de stream a partir de la URL original que guarda el backend.
// Las URLs tipo /play/xxxx (sin extensión de contenedor) o .ts son MPEG-TS en
// vivo; las .m3u8/.m3u son HLS. El backend hace proxy de ambos por la misma
// ruta /live/.../{id}.m3u8, así que la app debe forzar el extractor correcto.
fun streamKindOf(streamUrl: String): StreamKind {
    val lower = streamUrl.lowercase()
    if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) return StreamKind.HLS
    if (lower.endsWith(".ts") || lower.contains("/play/")) return StreamKind.TS
    return StreamKind.PROGRESSIVE
}

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

    // Propagate fullscreen state up so HomeScreen hides the nav rail/bar.
    LaunchedEffect(fullscreen) { onFullscreen(fullscreen) }

    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()

    val player = remember { TvPlayer(context) }
    LaunchedEffect(Unit) {
        if (filtered.isNotEmpty()) {
            val ch = filtered[0]
            player.prepare(catalog.liveUrl(ch.streamId), streamKindOf(ch.streamUrl))
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val playerView = remember { player.playerView(context) }
    val listState = rememberLazyListState()
    val overlayListState = rememberLazyListState()
    val overlayFocusRequester = remember { FocusRequester() }

    fun playIndex(i: Int) {
        selectedIndex = i
        if (filtered.isNotEmpty()) {
            val ch = filtered[i]
            player.prepare(catalog.liveUrl(ch.streamId), streamKindOf(ch.streamUrl))
        }
    }

    var numberBuffer by remember { mutableStateOf("") }
    LaunchedEffect(numberBuffer) {
        if (numberBuffer.isNotEmpty()) {
            delay(700)
            val n = numberBuffer.toIntOrNull()
            if (n != null && n in 1..filtered.size) {
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
        if (overlay) {
            overlayListState.scrollToItem(selectedIndex.coerceAtLeast(0))
            overlayFocusRequester.requestFocus()
        }
    }

    Box(Modifier.fillMaxSize().onKeyEvent { ev ->
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
                Key.DirectionCenter, Key.Enter -> {
                    if (fullscreen) { overlay = !overlay; true } else false
                }
                else -> false
            }
        } else false
    }) {
        if (fullscreen) {
            // ── Fullscreen playback ─────────────────────────────────────────
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        (playerView.parent as? ViewGroup)?.removeView(playerView)
                        android.widget.FrameLayout(ctx).also { fl ->
                            fl.addView(
                                playerView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    }
                )
                // Click to toggle channel list overlay
                Box(Modifier.fillMaxSize().clickable { overlay = !overlay })
                // Channel list overlay
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
                            .padding(16.dp)
                            .focusRequester(overlayFocusRequester)
                            .focusable(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filtered) { i, ch ->
                            ChannelRow(
                                index = i,
                                name = ch.name,
                                logo = ch.streamIcon,
                                onFocus = { playIndex(i) },
                                onClick = { playIndex(i); overlay = false }
                            )
                        }
                    }
                }
            }
        } else {
            // ── Split view ──────────────────────────────────────────────────
            Row(Modifier.fillMaxSize().background(Background)) {
                // ── Left: channel list ──────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxHeight()
                        .weight(0.42f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search field
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
                    // Category chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("Todos", state.selectedCategory == null) { vm.selectCategory(null) }
                        state.categories.forEach { (id, name) ->
                            Chip(name, state.selectedCategory == id) { vm.selectCategory(id) }
                        }
                    }
                    // Channel list
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

                // ── Right: live video panel ─────────────────────────────────
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(0.58f)
                        .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    // Video with rounded corners + shadow
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
                            .background(Color.Black)
                            .focusable()
                            .clickable { fullscreen = true }
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                (playerView.parent as? ViewGroup)?.removeView(playerView)
                                android.widget.FrameLayout(ctx).also { fl ->
                                    fl.addView(
                                        playerView,
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    )
                                }
                            }
                        )
                        // Transparent overlay to capture clicks on the video view and make it fullscreen
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable { fullscreen = true }
                        )
                        // Top gradient for channel name overlay
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
                        // Channel name
                        Text(
                            text = filtered.getOrNull(selectedIndex)?.name ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                        // Favorite button — circle icon
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
                            Text(
                                text = if (isFav) "★" else "☆",
                                color = if (isFav) Accent else Color(0xFFAAAAAA),
                                fontSize = 16.sp
                            )
                        }
                        // "Tap to fullscreen" hint at bottom-right
                        Text(
                            text = "⛶",
                            color = Color(0x88FFFFFF),
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
