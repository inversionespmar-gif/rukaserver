package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MoviesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val movies: List<VodStream> = emptyList()
)

class MoviesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(MoviesUiState())
    val state: StateFlow<MoviesUiState> = _state
    init { load() }
    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.vodStreams() }
                .onSuccess { _state.value = _state.value.copy(loading = false, movies = it) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }
}
