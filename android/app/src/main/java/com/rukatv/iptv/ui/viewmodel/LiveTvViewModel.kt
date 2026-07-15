package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.LiveStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LiveTvUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<Pair<String, String>> = emptyList(),
    val channels: List<LiveStream> = emptyList(),
    val selectedCategory: String? = null
)

class LiveTvViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(LiveTvUiState())
    val state: StateFlow<LiveTvUiState> = _state

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val cats = catalog.liveCategories()
                val chans = catalog.liveStreams()
                _state.value = _state.value.copy(
                    loading = false,
                    categories = cats.map { it.categoryId to it.categoryName },
                    channels = chans
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun selectCategory(id: String?) {
        _state.value = _state.value.copy(selectedCategory = id)
    }

    fun filteredChannels(): List<LiveStream> {
        val sel = _state.value.selectedCategory
        val all = _state.value.channels
        return if (sel == null) all else all.filter { it.categoryId == sel }
    }
}
