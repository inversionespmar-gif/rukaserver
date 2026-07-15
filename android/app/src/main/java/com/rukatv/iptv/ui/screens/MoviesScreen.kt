package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.MoviesViewModel
import kotlinx.coroutines.launch

@Composable
fun MoviesScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { MoviesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<VodStream?>(null) }
    if (selected != null) {
        MovieDetail(selected!!, catalog, favorites) { id, title -> onPlay(catalog.movieUrl(id), title) }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.movies) { m ->
            PosterCard(title = m.name, poster = m.poster) { selected = m }
        }
    }
}

@Composable
private fun MovieDetail(m: VodStream, catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (Long, String) -> Unit) {
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    val id = "movie:${m.streamId}"
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.Text(m.name, color = com.rukatv.iptv.ui.theme.Accent, fontSize = 24.sp)
        androidx.compose.material3.Text(m.plot, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
        androidx.compose.material3.Text("Rating: ${m.rating}", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 12.sp)
        androidx.compose.material3.Button(onClick = { onPlay(m.streamId, m.name) }) { androidx.compose.material3.Text("Reproducir") }
        androidx.compose.material3.Button(onClick = { scope.launch { favorites.toggle(id) } }) {
            androidx.compose.material3.Text(if (favSet.contains(id)) "★ Favorito" else "☆ Favorito")
        }
    }
}
