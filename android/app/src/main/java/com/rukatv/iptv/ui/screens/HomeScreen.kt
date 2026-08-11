package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.Screen
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.AppTopBar
import com.rukatv.iptv.ui.components.NavBar
import com.rukatv.iptv.ui.components.NavItem
import com.rukatv.iptv.ui.components.NavRail
import com.rukatv.iptv.ui.theme.Background

@Composable
fun HomeScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    progressStore: PlaybackProgressStore? = null,
    isTv: Boolean = false,
    onLogout: () -> Unit = {},
    onPlay: (String, String) -> Unit,
    onPlayQueue: (List<PlayItem>, Int) -> Unit
) {
    var current by remember { mutableStateOf(Screen.HOME) }
    var fullscreen by remember { mutableStateOf(false) }

    // ── Main navigation items ────────────────────────────────────────────────
    val navItems = listOf(
        NavItem("home",     "Inicio",    Icons.Filled.Home),
        NavItem("live",     "TV en Vivo",Icons.Filled.Tv),
        NavItem("movies",   "Películas", Icons.Filled.Movie),
        NavItem("series",   "Series",    Icons.Filled.VideoLibrary),
        NavItem("search",   "Buscar",    Icons.Filled.Search),
        NavItem("mylist",   "Mi Lista",  Icons.Filled.Favorite),
        NavItem("downloads","Descargas", Icons.Filled.Download),
        NavItem("recordings","Grabaciones", Icons.Filled.Videocam)
    )

    // ── Bottom items (TV rail only) ──────────────────────────────────────────
    val bottomNavItems = listOf(
        NavItem("settings", "Configuración", Icons.Filled.Settings)
    )

    // ── Phone bottom bar items (abbreviated) ────────────────────────────────
    val mobileNavItems = listOf(
        NavItem("home",   "Inicio",    Icons.Filled.Home),
        NavItem("live",   "TV",        Icons.Filled.Tv),
        NavItem("movies", "Películas", Icons.Filled.Movie),
        NavItem("series", "Series",    Icons.Filled.VideoLibrary),
        NavItem("search", "Buscar",    Icons.Filled.Search),
        NavItem("mylist", "Mi Lista",  Icons.Filled.Favorite)
    )

    val onSelectNav: (String) -> Unit = { key ->
        current = when (key.lowercase()) {
            "home"                  -> Screen.HOME
            "live"                  -> Screen.LIVE
            "movies"                -> Screen.MOVIES
            "series"                -> Screen.SERIES
            "search"                -> Screen.SEARCH
            "mylist", "favorites"   -> Screen.MYLIST
            "downloads"             -> Screen.MYLIST   // reuse MyList for now
            "recordings"            -> Screen.MYLIST
            "settings"              -> Screen.SETTINGS
            else                    -> Screen.HOME
        }
    }

    val selectedKey = when (current) {
        Screen.HOME                     -> "home"
        Screen.LIVE                     -> "live"
        Screen.MOVIES                   -> "movies"
        Screen.SERIES                   -> "series"
        Screen.SEARCH                   -> "search"
        Screen.FAVORITES, Screen.MYLIST -> "mylist"
        Screen.SETTINGS                 -> "settings"
    }

    // ── Content switcher ─────────────────────────────────────────────────────
    @Composable
    fun CurrentScreen() {
        when (current) {
            Screen.HOME    -> HomeContentScreen(
                catalog = catalog,
                favorites = favorites,
                progressStore = progressStore,
                onPlay = onPlay,
                onNavigate = onSelectNav
            )
            Screen.LIVE    -> LiveTvScreen(catalog, favorites, onPlay, onFullscreen = { fullscreen = it })
            Screen.MOVIES  -> MoviesScreen(catalog, favorites, progressStore, onPlay)
            Screen.SERIES  -> SeriesScreen(catalog, favorites, onPlay, onPlayQueue)
            Screen.SEARCH  -> SearchScreen(catalog, favorites, onPlay)
            Screen.FAVORITES, Screen.MYLIST -> MyListScreen(catalog, favorites, progressStore, onPlay)
            Screen.SETTINGS -> SettingsScreen(onLogout = onLogout)
        }
    }

    if (isTv) {
        // ── TV Layout: NavRail (left) + Content (right) ──────────────────────
        Row(Modifier.fillMaxSize().background(Background)) {
            if (!fullscreen) {
                NavRail(
                    items = navItems,
                    selected = selectedKey,
                    onSelect = onSelectNav,
                    bottomItems = bottomNavItems
                )
            }
            Column(Modifier.weight(1f)) {
                if (!fullscreen) {
                    AppTopBar(
                        showSlogan = false,
                        onSearchClick  = { current = Screen.SEARCH },
                        onProfileClick = { current = Screen.SETTINGS }
                    )
                }
                Box(Modifier.weight(1f)) {
                    CurrentScreen()
                }
            }
        }
    } else {
        // ── Mobile Layout: Content (top) + NavBar (bottom) ───────────────────
        Column(Modifier.fillMaxSize().background(Background)) {
            Box(Modifier.weight(1f)) {
                CurrentScreen()
            }
            if (!fullscreen) {
                NavBar(
                    items = mobileNavItems,
                    selected = selectedKey,
                    onSelect = onSelectNav
                )
            }
        }
    }
}
