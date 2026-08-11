package com.rukatv.iptv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.GlassBorder
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.theme.TextSecondary
import com.rukatv.iptv.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String) -> Unit
) {
    val vm = remember { SearchViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()

    val recentSearches = remember { listOf("Superman", "Misión Imposible", "Ballerina", "Godzilla") }
    val trendingSearches = remember {
        listOf("Cómo entrenar a tu dragón", "Thunderbolts*", "Destino Final", "Karate Kid", "El Contador 2")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Search bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .border(
                    width = 1.dp,
                    brush = if (state.query.isNotEmpty())
                        Brush.horizontalGradient(listOf(LogoBlue.copy(alpha = 0.5f), LogoViolet.copy(alpha = 0.4f)))
                    else
                        Brush.horizontalGradient(listOf(GlassBorder, GlassBorder)),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = if (state.query.isNotEmpty()) Accent else Color(0xFF4A5878),
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (state.query.isEmpty()) {
                    Text(
                        "Buscar películas, series, canales...",
                        color = Color(0xFF3A4560),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = state.query,
                    onValueChange = { vm.search(it) },
                    modifier = Modifier.fillMaxWidth().focusable(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Accent)
                )
            }
            AnimatedVisibility(visible = state.query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Limpiar búsqueda",
                    tint = Color(0xFF6B7A99),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { vm.search("") }
                )
            }
        }

        // ── Empty state: discovery ─────────────────────────────────────────────
        if (state.query.isEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Recent searches
                item {
                    SearchSection(title = "Búsquedas recientes") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { term ->
                                SearchChip(term = term, onClick = { vm.search(term) })
                            }
                        }
                    }
                }

                // Quick category tiles
                item {
                    SearchSection(title = "Explorar por categoría") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SearchCategoryTile(
                                icon = Icons.Filled.Movie,
                                label = "Películas",
                                color = LogoBlue,
                                modifier = Modifier.weight(1f)
                            ) { vm.search("Película") }
                            SearchCategoryTile(
                                icon = Icons.Filled.Tv,
                                label = "Series",
                                color = LogoViolet,
                                modifier = Modifier.weight(1f)
                            ) { vm.search("Serie") }
                            SearchCategoryTile(
                                icon = Icons.Filled.Star,
                                label = "Tendencias",
                                color = Color(0xFFFFC107),
                                modifier = Modifier.weight(1f)
                            ) { vm.search("Tendencia") }
                        }
                    }
                }

                // Trending searches
                item {
                    SearchSection(title = "Tendencias de búsqueda") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            trendingSearches.forEachIndexed { index, trend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { vm.search(trend) }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Rank number
                                    Text(
                                        text = "${index + 1}",
                                        color = Accent.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = "Tendencia",
                                        tint = Color(0xFFFF6B35),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = trend,
                                        color = Color(0xFFD1D9EB),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // ── Search results ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Live channels results
                if (state.live.isNotEmpty()) {
                    item {
                        Text(
                            "Canales en vivo (${state.live.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(state.live) { ch ->
                        ChannelRow(index = 0, name = ch.name, logo = ch.streamIcon) {
                            onPlay(catalog.liveUrl(ch.streamId), ch.name)
                        }
                    }
                }

                // Movies/series results
                if (state.movies.isNotEmpty()) {
                    item {
                        Text(
                            "Películas y Series (${state.movies.size})",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            contentPadding = PaddingValues(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(520.dp)
                        ) {
                            items(state.movies) { movie ->
                                PosterCard(
                                    title = movie.name,
                                    poster = movie.poster,
                                    rating = movie.displayRating,
                                    quality = movie.quality,
                                    year = movie.year,
                                    onClick = { onPlay(catalog.movieUrl(movie.streamId), movie.name) }
                                )
                            }
                        }
                    }
                }

                // Empty results
                if (state.live.isEmpty() && state.movies.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color(0xFF2A3550),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = "Sin resultados para \"${state.query}\"",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Prueba con otro término",
                                color = Color(0xFF3A4560),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun SearchChip(term: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0E1420))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(GlassBorder, GlassBorder)),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(term, color = Color(0xFFD1D9EB), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchCategoryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
