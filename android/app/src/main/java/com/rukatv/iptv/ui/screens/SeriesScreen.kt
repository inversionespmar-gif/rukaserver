package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.SeriesViewModel
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Composable
fun SeriesScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { SeriesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<SeriesItem?>(null) }
    if (selected != null) {
        SeriesDetail(selected!!, catalog, favorites, onPlay)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.series) { s -> PosterCard(title = s.name, poster = s.poster) { selected = s } }
    }
}

@Composable
private fun SeriesDetail(series: SeriesItem, catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val favId = "series:${series.seriesId}"
    var info by remember { mutableStateOf<com.rukatv.iptv.data.remote.dto.SeriesInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(series.seriesId) {
        runCatching { catalog.seriesInfo(series.seriesId) }
            .onSuccess { info = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }
    if (loading) return LoadingState()
    if (error != null) return ErrorState(error!!) {}
    val data = info ?: return
    Column(Modifier.fillMaxSize().background(Background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(series.name, color = com.rukatv.iptv.ui.theme.Accent, fontSize = 24.sp)
        Button(onClick = { CoroutineScope(Dispatchers.IO).launch { favorites.toggle(favId) } }) {
            Text(if (favSet.contains(favId)) "★ Favorito" else "☆ Favorito")
        }
        data.seasons.forEach { season ->
            Text("Temporada ${season.seasonNumber}", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(data.episodes[season.seasonNumber.toString()] ?: emptyList()) { ep ->
                    androidx.compose.material3.Text(
                        "${ep.episodeNum}. ${ep.title}",
                        color = androidx.compose.ui.graphics.Color(0xFFE8EEF7),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.rukatv.iptv.ui.theme.Surface)
                            .clickable { onPlay(catalog.seriesUrl(ep.streamId), "${series.name} S${season.seasonNumber}E${ep.episodeNum}") }
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}
