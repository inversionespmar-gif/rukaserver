package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rukatv.iptv.Screen
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.NavItem
import com.rukatv.iptv.ui.components.NavRail
import com.rukatv.iptv.ui.theme.Background

@Composable
fun HomeScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String) -> Unit
) {
    var current by remember { mutableStateOf(Screen.LIVE) }
    Row(Modifier.fillMaxSize().background(Background)) {
        NavRail(
            items = listOf(
                NavItem("live", "TV en vivo", "▣"),
                NavItem("movies", "Películas", "▦"),
                NavItem("series", "Series", "▤"),
                NavItem("search", "Buscar", "⌕"),
                NavItem("favorites", "Favoritos", "★")
            ),
            selected = current.name.lowercase(),
            onSelect = { key -> current = Screen.valueOf(key.uppercase()) }
        )
        when (current) {
            Screen.LIVE -> LiveTvScreen(catalog, favorites, onPlay)
            Screen.MOVIES -> MoviesScreen(catalog, favorites, onPlay)
            Screen.SERIES -> SeriesScreen(catalog, favorites, onPlay)
            Screen.SEARCH -> SearchScreen(catalog, favorites, onPlay)
            Screen.FAVORITES -> FavoritesScreen(catalog, favorites, onPlay)
        }
    }
}
