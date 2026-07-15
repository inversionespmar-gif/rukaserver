package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { SearchViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BasicTextField(
            value = state.query,
            onValueChange = { vm.search(it) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).focusable().padding(12.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            singleLine = true,
            decorationBox = { inner -> if (state.query.isEmpty()) Text("Buscar por nombre...", color = Color.Gray) else inner() }
        )
        Text("Canales", color = Accent, fontSize = 16.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.live) { ch ->
                ChannelRow(index = 0, name = ch.name, logo = ch.streamIcon) {
                    onPlay(catalog.liveUrl(ch.streamId), ch.name)
                }
            }
        }
        Text("Películas", color = Accent, fontSize = 16.sp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.movies) { m -> PosterCard(title = m.name, poster = m.poster) { onPlay(catalog.movieUrl(m.streamId), m.name) } }
        }
    }
}
