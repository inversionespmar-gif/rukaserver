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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.CategoryRow
import com.rukatv.iptv.ui.viewmodel.MoviesViewModel
import kotlinx.coroutines.launch

@Composable
fun MoviesScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String) -> Unit
) {
    val vm = remember { MoviesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<VodStream?>(null) }
    if (selected != null) {
        MovieDetail(
            m = selected!!,
            catalog = catalog,
            favorites = favorites,
            onBack = { selected = null }
        ) { id, title -> onPlay(catalog.movieUrl(id), title) }
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
            CategoryRowSection(
                row = row,
                onMovieClick = { selected = it },
                onShowMore = { vm.toggleCategory(row.title) }
            )
        }
        // Bottom padding
        item { Box(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun CategoryRowSection(
    row: CategoryRow,
    onMovieClick: (VodStream) -> Unit,
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
            items(row.items) { movie ->
                PosterCard(
                    title = movie.name,
                    poster = movie.poster,
                    width = 130
                ) { onMovieClick(movie) }
            }
        }
    }
}

@Composable
private fun MovieDetail(
    m: VodStream,
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onBack: () -> Unit,
    onPlay: (Long, String) -> Unit
) {
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()
    val id = "movie:${m.streamId}"

    BackHandler { onBack() }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {
        // Hero banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                if (m.poster.isNotBlank()) {
                    AsyncImage(
                        model = m.poster,
                        contentDescription = m.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Surface))
                }
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
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = m.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (m.rating.isNotBlank() && m.rating != "0") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("★", color = Accent, fontSize = 13.sp)
                            Text(m.rating, color = Color(0xFFCCCCCC), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Accent)
                        .clickable { onPlay(m.streamId, m.name) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▶", color = Color(0xFF06231F), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Reproducir", color = Color(0xFF06231F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val isFav = favSet.contains(id)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isFav) Accent else Color(0xFF444444),
                            RoundedCornerShape(6.dp)
                        )
                        .background(if (isFav) Accent.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { scope.launch { favorites.toggle(id) } }
                        .padding(vertical = 14.dp, horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFav) "★" else "☆",
                        color = if (isFav) Accent else Color(0xFF888888),
                        fontSize = 20.sp
                    )
                }
            }
        }

        // Description
        if (m.plot.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Descripción",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = m.plot,
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
