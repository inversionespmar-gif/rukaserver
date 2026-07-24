package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SeriesCategoryRow(
    val title: String,
    val items: List<SeriesItem>,
    val showAll: Boolean = false
)

data class SeriesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val allSeries: List<SeriesItem> = emptyList(),
    val rows: List<SeriesCategoryRow> = emptyList(),
    val expandedCategory: String? = null
)

class SeriesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SeriesUiState())
    val state: StateFlow<SeriesUiState> = _state

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.seriesList() }
                .onSuccess { series ->
                    _state.value = _state.value.copy(loading = false, allSeries = series)
                    buildRows(series)
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    fun toggleCategory(title: String) {
        val current = _state.value.expandedCategory
        _state.value = _state.value.copy(expandedCategory = if (current == title) null else title)
        buildRows(_state.value.allSeries)
    }

    private fun buildRows(series: List<SeriesItem>) {
        val expanded = _state.value.expandedCategory
        val rows = mutableListOf<SeriesCategoryRow>()

        // Estrenos 2026
        val estrenos2026 = series.filter { it.releaseDate.startsWith("2026") }
            .sortedByDescending { it.releaseDate }
        if (estrenos2026.isNotEmpty()) {
            val showAll = expanded == "Estrenos 2026"
            rows.add(SeriesCategoryRow("Estrenos 2026", if (showAll) estrenos2026 else estrenos2026.take(10), showAll))
        }

        // TMDB genre IDs for series
        val genres = listOf(
            10759 to "Acción",
            35 to "Comedia",
            18 to "Drama",
            10765 to "Ciencia Ficción",
            10768 to "War & Politics",
            9648 to "Misterio",
            10762 to "Kids",
            16 to "Animación"
        )

        for ((genreId, genreName) in genres) {
            val filtered = series.filter { it.genreIds.contains(genreId) }
                .sortedByDescending { it.releaseDate }
            if (filtered.isNotEmpty()) {
                val showAll = expanded == genreName
                rows.add(SeriesCategoryRow(genreName, if (showAll) filtered else filtered.take(10), showAll))
            }
        }

        // Anime: genre 16 (Animation) - filtered by name containing common anime indicators
        val animeKeywords = listOf("anime", "no hay", "one piece", "naruto", "dragon ball", "attack on titan",
            "demon slayer", "jujutsu kaisen", "my hero", "sword art", "death note", "fullmetal",
            "bleach", "hunter x hunter", "one punch", "chainsaw man", "spy x family")
        val anime = series.filter { s ->
            s.genreIds.contains(16) || animeKeywords.any { s.name.lowercase().contains(it) }
        }.sortedByDescending { it.releaseDate }
        if (anime.isNotEmpty()) {
            val showAll = expanded == "Anime"
            rows.add(SeriesCategoryRow("Anime", if (showAll) anime else anime.take(10), showAll))
        }

        // K-Drama: series with Korean-sounding names or from Korean networks
        val koreanIndicators = listOf("k-drama", "kdrama", "korean", "coreano", "korea",
            "kdrama", "k show", "kdrama")
        val kDrama = series.filter { s ->
            val name = s.name.lowercase()
            koreanIndicators.any { name.contains(it) } ||
            // Check for Korean characters (Hangul range)
            s.name.any { it.code in 0xAC00..0xD7AF || it.code in 0x1100..0x11FF || it.code in 0x3130..0x318F }
        }.sortedByDescending { it.releaseDate }
        if (kDrama.isNotEmpty()) {
            val showAll = expanded == "K-Drama"
            rows.add(SeriesCategoryRow("K-Drama", if (showAll) kDrama else kDrama.take(10), showAll))
        }

        _state.value = _state.value.copy(rows = rows)
    }
}
