package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.LiveStream
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val live: List<LiveStream> = emptyList(),
    val movies: List<VodStream> = emptyList()
)

class SearchViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun search(q: String) {
        _state.value = _state.value.copy(query = q)
        if (q.isBlank()) { _state.value = _state.value.copy(live = emptyList(), movies = emptyList()); return }
        viewModelScope.launch {
            runCatching {
                val live = catalog.liveStreams().filter { it.name.contains(q, true) }
                val movies = catalog.vodStreams().filter { it.name.contains(q, true) }
                _state.value = _state.value.copy(live = live, movies = movies)
            }
        }
    }
}
