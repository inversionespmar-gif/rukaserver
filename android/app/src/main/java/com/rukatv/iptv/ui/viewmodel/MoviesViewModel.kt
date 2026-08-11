package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.remote.dto.ContinueWatchingItem
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.ui.components.FilterState
import com.rukatv.iptv.ui.components.SortOption
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
    val featuredMovies: List<VodStream> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val rows: List<CategoryRow> = emptyList(),
    val filteredMovies: List<VodStream> = emptyList(),
    val filterState: FilterState = FilterState(),
    val showGridView: Boolean = false,
    val gridTitle: String = "Todas las películas",
    val genresList: List<String> = listOf("Todo", "Acción", "Comedia", "Drama", "Terror", "Romance", "Animación", "Documental", "Ciencia Ficción", "Thriller")
)

class MoviesViewModel(
    private val catalog: CatalogRepository,
    private val progressStore: PlaybackProgressStore? = null
) : ViewModel() {
    private val _state = MutableStateFlow(MoviesUiState())
    val state: StateFlow<MoviesUiState> = _state

    init {
        load()
        observeContinueWatching()
    }

    private fun observeContinueWatching() {
        if (progressStore == null) return
        viewModelScope.launch {
            progressStore.continueWatchingList.collect { list ->
                _state.value = _state.value.copy(continueWatching = list)
            }
        }
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.vodStreams() }
                .onSuccess { movies ->
                    val featured = movies.filter { it.isNewRelease || it.rating.toDoubleOrNull() ?: 0.0 > 7.5 }
                        .take(5)
                        .ifEmpty { movies.take(5) }

                    _state.value = _state.value.copy(
                        loading = false,
                        allMovies = movies,
                        featuredMovies = featured
                    )
                    applyFilters()
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    fun setGenre(genre: String) {
        val newState = _state.value.filterState.copy(selectedGenre = genre)
        _state.value = _state.value.copy(filterState = newState)
        applyFilters()
    }

    fun setFilterState(newFilter: FilterState) {
        _state.value = _state.value.copy(filterState = newFilter)
        applyFilters()
    }

    fun toggleGridView(show: Boolean, title: String = "Todas las películas") {
        _state.value = _state.value.copy(showGridView = show, gridTitle = title)
    }

    private fun applyFilters() {
        val movies = _state.value.allMovies
        val filter = _state.value.filterState

        var filtered = movies

        // 1. Filter by Genre
        if (filter.selectedGenre != "Todo") {
            val genreMap = mapOf(
                "Acción" to 28, "Comedia" to 35, "Drama" to 18,
                "Ciencia Ficción" to 878, "Terror" to 27, "Romance" to 10749,
                "Thriller" to 53, "Animación" to 16, "Documental" to 99
            )
            val genreId = genreMap[filter.selectedGenre]
            if (genreId != null) {
                filtered = filtered.filter { it.genreIds.contains(genreId) || it.name.contains(filter.selectedGenre, ignoreCase = true) }
            }
        }

        // 2. Filter by Year
        if (filter.selectedYear != "Todos") {
            filtered = when (filter.selectedYear) {
                "Clásicos" -> filtered.filter { (it.year.toIntOrNull() ?: 2025) < 2020 }
                else -> filtered.filter { it.year == filter.selectedYear }
            }
        }

        // 3. Filter by Quality
        if (filter.selectedQuality != "Todas") {
            filtered = filtered.filter { it.quality.equals(filter.selectedQuality, ignoreCase = true) }
        }

        // 4. Sort Options
        filtered = when (filter.sortOption) {
            SortOption.RECENT -> filtered.sortedByDescending { it.releaseDate }
            SortOption.POPULAR -> filtered.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            SortOption.RATING -> filtered.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            SortOption.TITLE_AZ -> filtered.sortedBy { it.name }
            SortOption.YEAR -> filtered.sortedByDescending { it.year }
        }

        val isFilteredMode = filter.selectedGenre != "Todo" || filter.selectedYear != "Todos" || filter.selectedQuality != "Todas"

        // Build home rows
        val rows = mutableListOf<CategoryRow>()

        // Estrenos
        val estrenos = movies.filter { it.isNewRelease }.sortedByDescending { it.releaseDate }
        if (estrenos.isNotEmpty()) {
            rows.add(CategoryRow("Estrenos", estrenos.take(12)))
        }

        // Tendencias
        val tendencias = movies.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        if (tendencias.isNotEmpty()) {
            rows.add(CategoryRow("Tendencias", tendencias.take(12)))
        }

        // Recomendadas
        val recomendadas = movies.shuffled().take(12)
        if (recomendadas.isNotEmpty()) {
            rows.add(CategoryRow("Recomendadas para ti", recomendadas))
        }

        // Genre Rows
        val genres = listOf(
            28 to "Acción",
            35 to "Comedia",
            18 to "Drama",
            878 to "Ciencia Ficción",
            27 to "Terror",
            10749 to "Romance",
            16 to "Animación"
        )

        for ((genreId, genreTitle) in genres) {
            val genreFiltered = movies.filter { it.genreIds.contains(genreId) }
            if (genreFiltered.isNotEmpty()) {
                rows.add(CategoryRow(genreTitle, genreFiltered.take(10)))
            }
        }

        _state.value = _state.value.copy(
            filteredMovies = filtered,
            rows = rows,
            showGridView = isFilteredMode
        )
    }
}

