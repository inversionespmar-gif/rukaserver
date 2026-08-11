package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.HeroCarousel
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.MoviesViewModel

/**
 * Dedicated Home/Inicio screen that shows a hero banner, continue watching section
 * and curated content rows — serving as the app's landing page.
 */
@Composable
fun HomeContentScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    progressStore: PlaybackProgressStore? = null,
    onPlay: (String, String, Long, String) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val vm = remember { MoviesViewModel(catalog, progressStore) }
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selectedMovie by remember { mutableStateOf<VodStream?>(null) }

    // Detail overlay
    if (selectedMovie != null) {
        MovieDetailScreen(
            movie = selectedMovie!!,
            allMovies = state.allMovies,
            catalog = catalog,
            favorites = favorites,
            onBack = { selectedMovie = null },
            onMovieClick = { selectedMovie = it },
            onPlay = { id, title, poster -> onPlay(catalog.movieUrl(id), title, id, poster) }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── 1. Hero banner ───────────────────────────────────────────────
            if (state.featuredMovies.isNotEmpty()) {
                item {
                    HeroCarousel(
                        featuredMovies = state.featuredMovies,
                        onPlay = { movie -> onPlay(catalog.movieUrl(movie.streamId), movie.name, movie.streamId, movie.poster) },
                        onDetails = { movie -> selectedMovie = movie }
                    )
                }
            }

            // ── 2. Continuar viendo ──────────────────────────────────────────
            if (state.continueWatching.isNotEmpty()) {
                item {
                    HomeSectionHeader(
                        title = "Continuar viendo",
                        onSeeAll = { onNavigate("movies") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        items(state.continueWatching) { item ->
                            PosterCard(
                                title = item.title,
                                poster = item.poster,
                                progressFraction = item.progressFraction,
                                onClick = {
                                    val url = if (item.isSeries) catalog.seriesUrl(item.streamId) else catalog.movieUrl(item.streamId)
                                    onPlay(url, item.title, item.streamId, item.poster)
                                }
                            )
                        }

                    }
                }
            }

            // ── 3. Category rows from Movies ─────────────────────────────────
            items(state.rows.take(4)) { row ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeSectionHeader(
                        title = row.title,
                        onSeeAll = { onNavigate("movies") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(row.items) { movie ->
                            PosterCard(
                                title = movie.name,
                                poster = movie.poster,
                                rating = movie.displayRating,
                                quality = movie.quality,
                                isNew = movie.isNewRelease,
                                year = movie.year,
                                onClick = { selectedMovie = movie }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ver más →",
            color = Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSeeAll() }
        )
    }
}
