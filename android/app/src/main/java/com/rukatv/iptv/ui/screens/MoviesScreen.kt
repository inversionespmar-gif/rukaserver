package com.rukatv.iptv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.rukatv.iptv.ui.components.FilterBar
import com.rukatv.iptv.ui.components.FilterDialog
import com.rukatv.iptv.ui.components.HeroCarousel
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.CategoryRow
import com.rukatv.iptv.ui.viewmodel.MoviesViewModel
import kotlinx.coroutines.launch

@Composable
fun MoviesScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    progressStore: PlaybackProgressStore? = null,
    onPlay: (String, String) -> Unit
) {
    val vm = remember { MoviesViewModel(catalog, progressStore) }
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selectedMovie by remember { mutableStateOf<VodStream?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Show floating Back-To-Top button when scrolled
    val showFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2 || gridState.firstVisibleItemIndex > 6
        }
    }

    // Detail Screen Overlay
    if (selectedMovie != null) {
        MovieDetailScreen(
            movie = selectedMovie!!,
            allMovies = state.allMovies,
            catalog = catalog,
            favorites = favorites,
            onBack = { selectedMovie = null },
            onMovieClick = { selectedMovie = it },
            onPlay = { id, title -> onPlay(catalog.movieUrl(id), title) }
        )
        return
    }

    // Advanced Filter Modal
    if (showFilterDialog) {
        FilterDialog(
            initialState = state.filterState,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilter -> vm.setFilterState(newFilter) }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Quick Filter Bar
            FilterBar(
                filterState = state.filterState,
                genres = state.genresList,
                onGenreSelect = { genre -> vm.setGenre(genre) },
                onOpenFilterDialog = { showFilterDialog = true }
            )

            // Main Content Area (Home Feed vs Full Grid View)
            if (state.showGridView) {
                // Full Grid View Mode (Search or Category results)
                BackHandler { vm.toggleGridView(false) }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.gridTitle} (${state.filteredMovies.size})",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Volver a Inicio",
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { vm.setGenre("Todo") }
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 125.dp),
                        state = gridState,
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.filteredMovies) { movie ->
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

            } else {
                // Modern Home Feed (Hero Carousel + Continue Watching + Horizontal Rows)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // 1. Featured Top Hero Carousel
                    if (state.featuredMovies.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 14.dp).padding(top = 6.dp)) {
                                HeroCarousel(
                                    featuredMovies = state.featuredMovies,
                                    onPlay = { movie -> onPlay(catalog.movieUrl(movie.streamId), movie.name) },
                                    onDetails = { movie -> selectedMovie = movie }
                                )
                            }
                        }
                    }

                    // 2. Continuar viendo Section (if progress exists)
                    if (state.continueWatching.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Continuar viendo",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(state.continueWatching) { item ->
                                        PosterCard(
                                            title = item.title,
                                            poster = item.poster,
                                            progressFraction = item.progressFraction,
                                            onClick = {
                                                val url = if (item.isSeries) catalog.seriesUrl(item.streamId) else catalog.movieUrl(item.streamId)
                                                onPlay(url, item.title)
                                            }
                                        )
                                    }

                                }
                            }
                        }
                    }

                    // 3. Category Rows (Estrenos, Tendencias, Recomendadas, Géneros)
                    items(state.rows) { row ->
                        CategoryRowSection(
                            row = row,
                            onMovieClick = { selectedMovie = it },
                            onShowMore = { vm.toggleGridView(true, row.title) }
                        )
                    }

                    // 4. "Todas las películas" Grid Button Footer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { vm.toggleGridView(true, "Todas las películas") }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Explorar todas las películas",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        // Floating Back-To-Top Button
        AnimatedVisibility(
            visible = showFab,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        gridState.animateScrollToItem(0)
                    }
                },
                containerColor = Accent,
                contentColor = Color(0xFF041E19),
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Volver al inicio",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryRowSection(
    row: CategoryRow,
    onMovieClick: (VodStream) -> Unit,
    onShowMore: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.title,
                color = Color.White,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ver más →",
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onShowMore() }
            )
        }

        // Horizontal Row of Poster Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
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
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}
