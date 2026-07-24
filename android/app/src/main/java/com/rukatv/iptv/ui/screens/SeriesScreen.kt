package com.rukatv.iptv.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    onPlay: (String, String) -> Unit,
    onPlayQueue: (List<PlayItem>, Int) -> Unit
) {
    val vm = remember { SeriesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<SeriesItem?>(null) }
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
        items(state.rows) { row ->
            SeriesCategoryRowSection(
                row = row,
                onSeriesClick = { selected = it },
                onShowMore = { vm.toggleCategory(row.title) }
            )
        }
        // Bottom padding
        item { Box(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SeriesCategoryRowSection(
    row: SeriesCategoryRow,
    onSeriesClick: (SeriesItem) -> Unit,
    onShowMore: () -> Unit
) {
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
            if (row.items.size > 10 || !row.showAll) {
                Text(
                    text = if (row.showAll) "Ver menos" else "Ver más",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onShowMore() }
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
private fun SeriesDetail(
    series: SeriesItem,
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit,
    onPlayQueue: (List<PlayItem>, Int) -> Unit
) {
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    val favId = "series:${series.seriesId}"
    var info by remember { mutableStateOf<com.rukatv.iptv.data.remote.dto.SeriesInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    BackHandler { onBack() }

    LaunchedEffect(series.seriesId) {
        runCatching { catalog.seriesInfo(series.seriesId) }
            .onSuccess { info = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }
    if (loading) return LoadingState()
    if (error != null) return ErrorState(error!!) {}
    val data = info ?: return

    // Flat ordered playlist across all seasons for autoplay-next (Netflix style).
    val playlist = remember(data) {
        data.seasons.flatMap { season ->
            (data.episodes[season.seasonNumber.toString()] ?: emptyList()).map { ep ->
                PlayItem(
                    url = catalog.seriesUrl(ep.streamId),
                    title = "${series.name} S${season.seasonNumber}E${ep.episodeNum}"
                )
            }
        }
    }
    val indexOf = { streamId: Long ->
        playlist.indexOfFirst { it.url == catalog.seriesUrl(streamId) }.coerceAtLeast(0)
    }
    val isFav = favSet.contains(favId)

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
                    Text("←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                    Text("▶", color = Color(0xFF06231F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                            Text(
                                text = if (isFav) "★ Favorito" else "☆ Favorito",
                                color = if (isFav) Accent else Color(0xFF888888),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Episodes by season
        data.seasons.forEach { season ->
            item {
                Text(
                    text = "Temporada ${season.seasonNumber}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 8.dp)
                )
            }
            val episodes = data.episodes[season.seasonNumber.toString()] ?: emptyList()
            items(episodes) { ep ->
                EpisodeRow(
                    episodeNum = ep.episodeNum,
                    title = ep.title,
                    onClick = { onPlayQueue(playlist, indexOf(ep.streamId)) }
                )
            }
        }

        // Bottom spacing
        item { Box(Modifier.height(36.dp)) }
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
        Text(
            text = "▶",
            color = if (focused) Accent else Color(0xFF3A4050),
            fontSize = 11.sp
        )
    }
}
