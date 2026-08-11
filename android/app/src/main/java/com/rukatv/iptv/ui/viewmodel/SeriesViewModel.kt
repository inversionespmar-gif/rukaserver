package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SeriesCategoryRow(
    val title: String,
    val items: List<SeriesItem>,
    val showAll: Boolean = false,
    val totalCount: Int = 0
)

data class SeriesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val allSeries: List<SeriesItem> = emptyList(),
    val featuredSeries: List<SeriesItem> = emptyList(),
    val rows: List<SeriesCategoryRow> = emptyList(),
    val selectedGenre: String = "Todo",
    val genresList: List<String> = listOf("Todo"),
    // Map from categoryId -> categoryName
    val categoryMap: Map<String, String> = emptyMap(),
    // Category navigation: null = category list, non-null = detail screen
    val selectedCategory: String? = null
)

class SeriesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SeriesUiState())
    val state: StateFlow<SeriesUiState> = _state

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                // Load categories and series in parallel
                val categoriesDeferred = async { catalog.seriesCategories() }
                val seriesDeferred = async { catalog.seriesList() }

                val categories = runCatching { categoriesDeferred.await() }.getOrElse { emptyList() }
                val series = seriesDeferred.await()

                val categoryMap = categories.associate { it.categoryId to it.categoryName }

                val featured = series.filter { it.rating.toDoubleOrNull() ?: 0.0 > 7.5 }
                    .take(5)
                    .ifEmpty { series.take(5) }

                // Build genre list from categories for filter chips
                val genresList = mutableListOf("Todo")
                genresList.addAll(categories.map { it.categoryName }.filter { it.isNotBlank() })

                _state.value = _state.value.copy(
                    loading = false,
                    allSeries = series,
                    featuredSeries = featured,
                    categoryMap = categoryMap,
                    genresList = genresList
                )
                buildRows(series, categoryMap)
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun setGenre(genre: String) {
        _state.value = _state.value.copy(selectedGenre = genre, selectedCategory = null)
        buildRows(_state.value.allSeries, _state.value.categoryMap)
    }

    fun navigateToCategory(title: String) {
        _state.value = _state.value.copy(selectedCategory = title)
    }

    fun clearSelectedCategory() {
        _state.value = _state.value.copy(selectedCategory = null)
    }

    private fun buildRows(series: List<SeriesItem>, categoryMap: Map<String, String>) {
        val selectedGenre = _state.value.selectedGenre
        val rows = mutableListOf<SeriesCategoryRow>()

        // Filter by selected genre
        val filteredSeries = if (selectedGenre == "Todo") {
            series
        } else {
            // Find categoryId that matches the genre name
            val targetCatId = categoryMap.entries
                .firstOrNull { it.value.equals(selectedGenre, ignoreCase = true) }
                ?.key
            if (targetCatId != null) {
                series.filter { it.categoryId == targetCatId }
            } else {
                // Fallback: filter by name containing genre
                series.filter { s ->
                    s.categoryName.contains(selectedGenre, ignoreCase = true) ||
                    s.name.contains(selectedGenre, ignoreCase = true)
                }
            }
        }

        if (selectedGenre != "Todo") {
            // Single expanded category for filtered view
            val sorted = filteredSeries.sortedByDescending { it.releaseDate }
            if (sorted.isNotEmpty()) {
                rows.add(SeriesCategoryRow(selectedGenre, sorted, showAll = true, totalCount = sorted.size))
            }
            _state.value = _state.value.copy(rows = rows)
            return
        }

        // "Todo" mode: group by server category using categoryId
        // First add "Estrenos 2026" special row
        val estrenos = series
            .filter { it.releaseDate.startsWith("2026") }
            .sortedByDescending { it.releaseDate }
        if (estrenos.isNotEmpty()) {
            rows.add(SeriesCategoryRow(
                "Estrenos 2026",
                estrenos.take(10),
                showAll = false,
                estrenos.size
            ))
        }

        // Group by categoryId from server
        if (categoryMap.isNotEmpty()) {
            // Use the category order from categoryMap
            for ((catId, catName) in categoryMap) {
                if (catName.isBlank()) continue
                val catSeries = series
                    .filter { it.categoryId == catId }
                    .sortedByDescending { it.releaseDate }
                if (catSeries.isNotEmpty()) {
                    rows.add(SeriesCategoryRow(
                        catName,
                        catSeries.take(10),
                        showAll = false,
                        catSeries.size
                    ))
                }
            }
        } else {
            // Fallback if categories API failed: group by categoryName field in series
            val grouped = series
                .groupBy { it.categoryName.ifBlank { "General" } }
                .toSortedMap()
            for ((catName, catSeries) in grouped) {
                val sorted = catSeries.sortedByDescending { it.releaseDate }
                rows.add(SeriesCategoryRow(
                    catName,
                    sorted.take(10),
                    showAll = false,
                    sorted.size
                ))
            }
        }

        _state.value = _state.value.copy(rows = rows)
    }
}
