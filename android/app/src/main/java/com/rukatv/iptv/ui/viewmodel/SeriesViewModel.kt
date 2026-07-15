package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SeriesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val series: List<SeriesItem> = emptyList()
)

class SeriesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SeriesUiState())
    val state: StateFlow<SeriesUiState> = _state
    init { load() }
    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.seriesList() }
                .onSuccess { _state.value = _state.value.copy(loading = false, series = it) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }
}
