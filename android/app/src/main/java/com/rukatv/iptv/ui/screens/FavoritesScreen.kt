package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { FavoritesViewModel(catalog, favorites) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.items) { f ->
            PosterCard(title = f.name, poster = f.image) {
                // resolve URL from key prefix
                when {
                    f.key.startsWith("live:") -> onPlay(catalog.liveUrl(f.key.removePrefix("live:").toLongOrNull() ?: 0), f.name)
                    f.key.startsWith("movie:") -> onPlay(catalog.movieUrl(f.key.removePrefix("movie:").toLongOrNull() ?: 0), f.name)
                }
            }
        }
    }
}
