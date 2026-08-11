package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.FavoriteItem
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val loading: Boolean = true,
    val items: List<FavoriteItem> = emptyList()
)

class FavoritesViewModel(
    private val catalog: CatalogRepository,
    private val favorites: FavoritesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state

    init {
        viewModelScope.launch {
            favorites.favorites.collect { ids ->
                _state.value = _state.value.copy(loading = true)
                
                val live = runCatching { catalog.liveStreams() }.getOrDefault(emptyList())
                    .filter { ids.contains("live:${it.streamId}") }
                    .map { FavoriteItem("live:${it.streamId}", it.name, it.streamIcon, "live:${it.streamId}") }
                
                val movies = runCatching { catalog.vodStreams() }.getOrDefault(emptyList())
                    .filter { ids.contains("movie:${it.streamId}") }
                    .map { FavoriteItem("movie:${it.streamId}", it.name, it.poster, "movie:${it.streamId}") }
                
                val series = runCatching { catalog.seriesList() }.getOrDefault(emptyList())
                    .filter { ids.contains("series:${it.seriesId}") }
                    .map { FavoriteItem("series:${it.seriesId}", it.name, it.poster.ifBlank { it.cover }, "series:${it.seriesId}") }

                _state.value = _state.value.copy(loading = false, items = live + movies + series)
            }
        }
    }
}

