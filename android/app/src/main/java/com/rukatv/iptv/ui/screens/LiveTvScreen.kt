package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.player.TvPlayer
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

@Composable
fun LiveTvScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String) -> Unit
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

    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()

    val player = remember { TvPlayer(context) }
    LaunchedEffect(Unit) {
        if (filtered.isNotEmpty()) player.prepare(catalog.liveUrl(filtered[0].streamId))
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val playerView = remember { player.playerView(context) }
    val listState = rememberLazyListState()

    fun playIndex(i: Int) {
        selectedIndex = i
        if (filtered.isNotEmpty()) player.prepare(catalog.liveUrl(filtered[i].streamId))
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

    Box(Modifier.fillMaxSize().onKeyEvent { ev ->
        if (ev.type == KeyEventType.KeyDown) {
            val c = ev.nativeKeyEvent.unicodeChar.toChar()
            if (c.isDigit()) {
                numberBuffer += c
                true
            } else false
        } else false
    }) {
        if (fullscreen) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                // BACK: single shared PlayerView
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        (playerView.parent as? ViewGroup)?.removeView(playerView)
                        android.widget.FrameLayout(ctx).also { fl ->
                            fl.addView(playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        }
                    }
                )
                // MIDDLE: transparent click-through toggle
                Box(Modifier.fillMaxSize().clickable { overlay = !overlay }) {}
                // TOP: overlay list (declared after the toggle Box)
                if (overlay) {
                    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { overlay = false }.padding(16.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }
        } else {
            Row(Modifier.fillMaxSize().background(Background)) {
                // Left: enumerated channel list
                Column(Modifier.fillMaxHeight().weight(0.42f).padding(12.dp)) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).focusable().padding(10.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        singleLine = true,
                        decorationBox = { inner -> if (query.isEmpty()) Text("Filtrar canales...", color = Color.Gray) else inner() }
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Chip("Todos", state.selectedCategory == null) { vm.selectCategory(null) }
                        state.categories.forEach { (id, name) ->
                            Chip(name, state.selectedCategory == id) { vm.selectCategory(id) }
                        }
                    }
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                // Right: auto-play side player
                Box(
                    Modifier.fillMaxHeight().weight(0.58f).padding(12.dp)
                        .background(Color.Black).focusable()
                        .clickable { fullscreen = true }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            (playerView.parent as? ViewGroup)?.removeView(playerView)
                            android.widget.FrameLayout(ctx).also { fl ->
                                fl.addView(playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                            }
                        }
                    )
                    Text(
                        filtered.getOrNull(selectedIndex)?.name ?: "",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    )
                    val favKey = "live:${filtered.getOrNull(selectedIndex)?.streamId}"
                    Button(
                        onClick = { scope.launch { favorites.toggle(favKey) } },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    ) {
                        Text(if (favSet.contains(favKey)) "★" else "☆")
                    }
                }
            }
        }
    }
}
