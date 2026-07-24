package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CategoryRow(
    val title: String,
    val items: List<VodStream>,
    val showAll: Boolean = false
)

data class MoviesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val allMovies: List<VodStream> = emptyList(),
    val rows: List<CategoryRow> = emptyList(),
    val expandedCategory: String? = null
)

class MoviesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(MoviesUiState())
    val state: StateFlow<MoviesUiState> = _state

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.vodStreams() }
                .onSuccess { movies ->
                    _state.value = _state.value.copy(loading = false, allMovies = movies)
                    buildRows(movies)
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    fun toggleCategory(title: String) {
        val current = _state.value.expandedCategory
        _state.value = _state.value.copy(expandedCategory = if (current == title) null else title)
        buildRows(_state.value.allMovies)
    }

    private fun buildRows(movies: List<VodStream>) {
        val expanded = _state.value.expandedCategory
        val rows = mutableListOf<CategoryRow>()

        // Estrenos 2026
        val estrenos2026 = movies.filter { it.releaseDate.startsWith("2026") }
            .sortedByDescending { it.releaseDate }
        if (estrenos2026.isNotEmpty()) {
            val showAll = expanded == "Estrenos 2026"
            rows.add(CategoryRow("Estrenos 2026", if (showAll) estrenos2026 else estrenos2026.take(10), showAll))
        }

        // TMDB genre IDs
        val genres = listOf(
            28 to "Acción",
            35 to "Comedia",
            18 to "Drama",
            878 to "Ciencia Ficción",
            27 to "Terror",
            10749 to "Romance",
            53 to "Thriller",
            16 to "Animación"
        )

        for ((genreId, genreName) in genres) {
            val filtered = movies.filter { it.genreIds.contains(genreId) }
                .sortedByDescending { it.releaseDate }
            if (filtered.isNotEmpty()) {
                val showAll = expanded == genreName
                rows.add(CategoryRow(genreName, if (showAll) filtered else filtered.take(10), showAll))
            }
        }

        _state.value = _state.value.copy(rows = rows)
    }
}
