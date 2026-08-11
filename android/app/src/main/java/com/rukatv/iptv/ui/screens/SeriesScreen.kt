package com.rukatv.iptv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.SeriesCategoryRow
import com.rukatv.iptv.ui.viewmodel.SeriesViewModel
import kotlinx.coroutines.launch

@Composable
fun SeriesScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String, Long, String) -> Unit,
    onPlayQueue: (List<PlayItem>, Int) -> Unit
) {
    val vm = remember { SeriesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<SeriesItem?>(null) }
    // Use the ViewModel's selectedCategory state for navigation (fixes "Ver más" button)
    val selectedCategory = state.selectedCategory
    if (selectedCategory != null) {
        // Pass ALL series for this category (not just the 10 preview items)
        val allSeriesInCategory = if (selectedCategory == "Estrenos 2026") {
            state.allSeries.filter { it.releaseDate.startsWith("2026") }
                .sortedByDescending { it.releaseDate }
        } else {
            val catId = state.categoryMap.entries
                .firstOrNull { it.value == selectedCategory }?.key
            if (catId != null) {
                state.allSeries.filter { it.categoryId == catId }
                    .sortedByDescending { it.releaseDate }
            } else {
                state.allSeries.filter { it.categoryName == selectedCategory }
                    .sortedByDescending { it.releaseDate }
            }
        }
        CategoryDetailScreen(
            title = selectedCategory,
            allSeriesInCategory = allSeriesInCategory,
            catalog = catalog,
            favorites = favorites,
            onBack = { vm.clearSelectedCategory() },
            onPlay = onPlay,
            onPlayQueue = onPlayQueue
        )
        return
    }

    if (selected != null) {
        SeriesDetail(
            series = selected!!,
            catalog = catalog,
            favorites = favorites,
            onBack = { selected = null },
            onPlay = onPlay,
            onPlayQueue = onPlayQueue
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Carousel for featured series
        if (state.featuredSeries.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 14.dp).padding(top = 6.dp)) {
                    SeriesHeroCarousel(
                        featuredSeries = state.featuredSeries,
                        onPlay = { series -> selected = series },
                        onDetails = { series -> selected = series }
                    )
                }
            }
        }

        // Genre filter chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.genresList) { genre ->
                    val isSelected = state.selectedGenre == genre
                    SeriesGenreChip(
                        text = genre,
                        isSelected = isSelected,
                        onClick = { vm.setGenre(genre) }
                    )
                }
            }
        }

        // Category rows
        items(state.rows) { row ->
            SeriesCategoryRowSection(
                row = row,
                onSeriesClick = { selected = it },
                onShowMore = { vm.navigateToCategory(row.title) }
            )
        }
        // Also handle "Ver menos" within a category row (toggle)

        // Bottom padding
        item { Box(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SeriesGenreChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    val bgColor = when {
        isSelected -> Accent
        focused -> Color(0xFF2D3748)
        else -> Color(0xFF1A202C)
    }

    val textColor = when {
        isSelected -> Color(0xFF041E19)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (focused) 2.dp else if (isSelected) 0.dp else 1.dp,
                color = if (focused) Accent else Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SeriesHeroCarousel(
    featuredSeries: List<SeriesItem>,
    modifier: Modifier = Modifier,
    onPlay: (SeriesItem) -> Unit,
    onDetails: (SeriesItem) -> Unit
) {
    if (featuredSeries.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    val currentSeries = featuredSeries[currentIndex]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F1519))
    ) {
        // Crossfade animation for backdrop change
        Crossfade(
            targetState = currentSeries,
            animationSpec = tween(600),
            label = "seriesHeroCrossfade"
        ) { series ->
            Box(modifier = Modifier.fillMaxSize()) {
                val imageUrl = if (series.poster.isNotBlank()) series.poster else series.cover
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E262B)))
                }

                // Smooth Gradient Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x33000000),
                                    0.4f to Color(0x66000000),
                                    0.75f to Color(0xDD0B0F12),
                                    1.0f to Color(0xFF0B0F12)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0xCC0B0F12),
                                    0.5f to Color(0x660B0F12),
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Hero Content Info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentSeries.releaseDate.startsWith("2026")) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Accent)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "ESTRENO",
                            color = Color(0xFF041E19),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = "${currentSeries.year}  •  ${currentSeries.seasonsCount} temporadas",
                    color = Color(0xFFCBD5E0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Title
            Text(
                text = currentSeries.name,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Rating & Plot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Valoración",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = currentSeries.displayRating,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "  ${currentSeries.plot.ifBlank { "Una serie imperdible disponible en HD y 4K." }}",
                    color = Color(0xFFA0AEC0),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Action Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                HeroButton(
                    text = "Ver ahora",
                    icon = Icons.Filled.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlay(currentSeries) }
                )

                HeroButton(
                    text = "Más info",
                    icon = Icons.Filled.Info,
                    isPrimary = false,
                    onClick = { onDetails(currentSeries) }
                )
            }
        }

        // Carousel Indicators (Dots)
        if (featuredSeries.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                featuredSeries.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Accent else Color(0x66FFFFFF))
                            .clickable { currentIndex = index }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    val bgColor = when {
        focused -> Accent
        isPrimary -> Color.White
        else -> Color(0x44334155)
    }
    val tintColor = when {
        focused -> Color(0xFF041E19)
        isPrimary -> Color(0xFF0F172A)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Accent else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .shadow(if (focused) 8.dp else 0.dp)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )
            Text(text, color = tintColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SeriesCategoryRowSection(
    row: SeriesCategoryRow,
    onSeriesClick: (SeriesItem) -> Unit,
    onShowMore: () -> Unit
) {
    val showMoreInteraction = remember { MutableInteractionSource() }
    val showMoreFocused = showMoreInteraction.collectIsFocusedAsState().value

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Category header
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showMoreFocused) Accent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = if (showMoreFocused) 1.dp else 0.dp,
                        color = if (showMoreFocused) Accent else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .focusable(interactionSource = showMoreInteraction)
                    .clickable(interactionSource = showMoreInteraction, indication = null) { onShowMore() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (row.showAll) "Ver menos" else if (row.totalCount > 0) "Ver más (${row.totalCount})" else "Ver más",
                    color = if (showMoreFocused) Color.White else Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Horizontal row of posters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(row.items) { series ->
                PosterCard(
                    title = series.name,
                    poster = series.poster,
                    width = 130
                ) { onSeriesClick(series) }
            }
        }
    }
}

@Composable
internal fun SeriesDetail(
    series: SeriesItem,
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onBack: () -> Unit,
    onPlay: (String, String, Long, String) -> Unit,
    onPlayQueue: (List<PlayItem>, Int) -> Unit
) {
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    val favId = "series:${series.seriesId}"
    var info by remember { mutableStateOf<com.rukatv.iptv.data.remote.dto.SeriesInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedSeason by remember { mutableIntStateOf(1) }

    BackHandler { onBack() }

    LaunchedEffect(series.seriesId) {
        runCatching { catalog.seriesInfo(series.seriesId) }
            .onSuccess { info = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }
    if (loading) return LoadingState()
    if (error != null) return ErrorState(error!!) {}
    val data = info ?: return

    // Set initial season to first available
    LaunchedEffect(data.seasons) {
        if (data.seasons.isNotEmpty() && selectedSeason > data.seasons.size) {
            selectedSeason = data.seasons.firstOrNull()?.seasonNumber ?: 1
        }
    }

    // Flat ordered playlist across all seasons for autoplay-next
    val playlist = remember(data) {
        data.seasons.flatMap { season ->
            (data.episodes[season.seasonNumber.toString()] ?: emptyList()).map { ep ->
                PlayItem(
                    url = catalog.seriesUrl(ep.streamId),
                    title = "${series.name} S${season.seasonNumber}E${ep.episodeNum}",
                    streamId = ep.streamId,
                    poster = series.poster
                )
            }
        }
    }
    val indexOf = { streamId: Long ->
        playlist.indexOfFirst { it.url == catalog.seriesUrl(streamId) }.coerceAtLeast(0)
    }
    val isFav = favSet.contains(favId)

    // Current season episodes
    val currentEpisodes = data.episodes[selectedSeason.toString()] ?: emptyList()

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {
        // Hero banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (series.poster.isNotBlank()) {
                    AsyncImage(
                        model = series.poster,
                        contentDescription = series.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Surface))
                }
                // Bottom fade to background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x33000000),
                                    0.45f to Color.Transparent,
                                    1.0f to Background
                                )
                            )
                        )
                )
                // Back button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
                        .clickable { onBack() }
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Title + action buttons at bottom of hero
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = series.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Play from beginning
                        if (playlist.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Accent)
                                    .clickable { onPlayQueue(playlist, 0) }
                                    .padding(horizontal = 18.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color(0xFF06231F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Reproducir", color = Color(0xFF06231F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        // Favorite button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(
                                    1.dp,
                                    if (isFav) Accent else Color(0xFF555555),
                                    RoundedCornerShape(6.dp)
                                )
                                .background(if (isFav) Accent.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable { scope.launch { favorites.toggle(favId) } }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Favorito",
                                    tint = if (isFav) Accent else Color(0xFF888888),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Favorito",
                                    color = if (isFav) Accent else Color(0xFF888888),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Season selector (horizontal chips)
        if (data.seasons.size > 1) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(data.seasons) { index, season ->
                        val isSelected = selectedSeason == season.seasonNumber
                        SeasonChip(
                            text = "Temporada ${season.seasonNumber}",
                            isSelected = isSelected,
                            onClick = { selectedSeason = season.seasonNumber }
                        )
                    }
                }
            }
        } else if (data.seasons.isNotEmpty()) {
            item {
                Text(
                    text = "Temporada ${data.seasons.first().seasonNumber}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 8.dp)
                )
            }
        }

        // Episodes for selected season
        items(currentEpisodes) { ep ->
            EpisodeRow(
                episodeNum = ep.episodeNum,
                title = ep.title,
                onClick = { onPlayQueue(playlist, indexOf(ep.streamId)) }
            )
        }

        // Bottom spacing
        item { Box(Modifier.height(36.dp)) }
    }
}

@Composable
private fun SeasonChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    val bgColor = when {
        isSelected -> Accent
        focused -> Color(0xFF2D3748)
        else -> Color(0xFF1A202C)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (focused) 2.dp else if (isSelected) 0.dp else 1.dp,
                color = if (focused) Accent else Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF041E19) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EpisodeRow(
    episodeNum: Int,
    title: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Accent.copy(alpha = 0.10f) else Surface)
            .border(
                width = if (focused) 1.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Episode number
        Text(
            text = "$episodeNum",
            color = if (focused) Accent else Color(0xFF4B5563),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        // Title
        Text(
            text = title.ifBlank { "Episodio $episodeNum" },
            color = if (focused) Color(0xFFE8EEF7) else Color(0xFFBBBBBB),
            fontSize = 14.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        // Play icon
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Reproducir",
            tint = if (focused) Accent else Color(0xFF3A4050),
            modifier = Modifier.size(16.dp)
        )
    }
}
