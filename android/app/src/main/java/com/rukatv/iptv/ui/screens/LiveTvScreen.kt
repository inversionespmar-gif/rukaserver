package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.player.TvPlayer
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.Chip
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.LiveTvViewModel

@Composable
fun LiveTvScreen(catalog: CatalogRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { LiveTvViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    val channels = vm.filteredChannels()
    var selectedIndex by remember { mutableStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }

    val player = remember {
        TvPlayer(context).apply {
            if (channels.isNotEmpty()) prepare(catalog.liveUrl(channels[0].streamId))
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val playerView = remember { player.playerView(context) }

    fun playIndex(i: Int) {
        selectedIndex = i
        if (channels.isNotEmpty()) player.prepare(catalog.liveUrl(channels[i].streamId))
    }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    (playerView.parent as? ViewGroup)?.removeView(playerView)
                    android.widget.FrameLayout(ctx).also { fl ->
                        fl.addView(playerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                }
            )
            if (overlay) {
                Box(Modifier.fillMaxSize().background(Color(0xCC000000)).padding(16.dp)) {
                    LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(channels) { i, ch ->
                            ChannelRow(index = i, name = ch.name, logo = ch.streamIcon) {
                                playIndex(i); overlay = false
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize().clickable { overlay = !overlay }) {}
        }
    } else {
        Row(Modifier.fillMaxSize().background(Background)) {
            // Left: enumerated channel list
            Column(Modifier.fillMaxHeight().weight(0.42f).padding(12.dp)) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Chip("Todos", state.selectedCategory == null) { vm.selectCategory(null) }
                    state.categories.forEach { (id, name) ->
                        Chip(name, state.selectedCategory == id) { vm.selectCategory(id) }
                    }
                }
                LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(channels) { i, ch ->
                        ChannelRow(index = i, name = ch.name, logo = ch.streamIcon) { playIndex(i) }
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
                androidx.compose.material3.Text(
                    channels.getOrNull(selectedIndex)?.name ?: "",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                )
            }
        }
    }
}
