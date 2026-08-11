package com.rukatv.iptv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface

/**
 * Shows ALL series of a given category in a vertical grid.
 * Clicking a series opens SeriesDetail (not a direct play),
 * so the user can choose season/episode first.
 */
@Composable
fun CategoryDetailScreen(
    title: String,
    allSeriesInCategory: List<SeriesItem>,
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onBack: () -> Unit,
    onPlay: (String, String) -> Unit,
    onPlayQueue: (List<PlayItem>, Int) -> Unit
) {
    BackHandler { onBack() }

    // When a series is tapped, show its detail screen (season/episode picker)
    var selectedSeries by remember { mutableStateOf<SeriesItem?>(null) }

    if (selectedSeries != null) {
        SeriesDetail(
            series = selectedSeries!!,
            catalog = catalog,
            favorites = favorites,
            onBack = { selectedSeries = null },
            onPlay = onPlay,
            onPlayQueue = onPlayQueue
        )
        return
    }

    val heroItem = allSeriesInCategory.firstOrNull()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 0.dp,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Item 0: Hero banner header (scrolls naturally with the grid) ───────
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(bottom = 10.dp)
                ) {
                    if (heroItem != null && heroItem.poster.isNotBlank()) {
                        AsyncImage(
                            model = heroItem.poster,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Surface))
                    }

                    // Gradient fade to background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color(0x55000000),
                                        0.5f to Color(0x33000000),
                                        0.85f to Color(0xCC0B0F12),
                                        1.0f to Background
                                    )
                                )
                            )
                    )

                    // Category title + count
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${allSeriesInCategory.size} series",
                            color = Color(0xFFCBD5E0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Grid items: Series posters ─────────────────────────────────────────
            items(allSeriesInCategory) { series ->
                PosterCard(
                    title = series.name,
                    poster = series.poster,
                    width = 120,
                    height = 180,
                    rating = series.displayRating,
                    year = series.year,
                    onClick = { selectedSeries = series }
                )
            }
        }

        // ── Floating Volver Button (stays at top left with glassmorphism) ─────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(Color(0xCC0B0F12))
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable { onBack() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text("Volver", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}