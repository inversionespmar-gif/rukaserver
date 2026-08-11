package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.FavoritesViewModel
import kotlinx.coroutines.launch

@Composable
fun MyListScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    progressStore: PlaybackProgressStore? = null,
    onPlay: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Mi Lista, 1 = Continuar viendo

    val vm = remember { FavoritesViewModel(catalog, favorites) }
    val favState by vm.state.collectAsStateWithLifecycle()

    val continueWatchingList by (progressStore?.continueWatchingList?.collectAsStateWithLifecycle(emptyList())
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) })

    val scope = rememberCoroutineScope()

    if (favState.loading) return LoadingState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(top = 16.dp)
    ) {
        // Tab Selector Header (Mi Lista vs Continuar Viendo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabPill(
                icon = Icons.Filled.Favorite,
                title = "Mi lista (${favState.items.size})",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )

            TabPill(
                icon = Icons.Filled.PlayCircle,
                title = "Continuar (${continueWatchingList.size})",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area based on Tab
        if (selectedTab == 0) {
            // Tab 0: Mi Lista (Favorites)
            if (favState.items.isEmpty()) {
                EmptyStateView("Tu lista está vacía", "Agrega tus películas y series favoritas para acceder a ellas rápidamente.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 125.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favState.items) { f ->
                        PosterCard(
                            title = f.name,
                            poster = f.image,
                            onClick = {
                                when {
                                    f.key.startsWith("live:") -> onPlay(catalog.liveUrl(f.key.removePrefix("live:").toLongOrNull() ?: 0), f.name)
                                    f.key.startsWith("movie:") -> onPlay(catalog.movieUrl(f.key.removePrefix("movie:").toLongOrNull() ?: 0), f.name)
                                    f.key.startsWith("series:") -> {
                                        val sId = f.key.removePrefix("series:").toLongOrNull() ?: 0
                                        if (sId > 0) {
                                            scope.launch {
                                                val info = runCatching { catalog.seriesInfo(sId) }.getOrNull()
                                                val firstEp = info?.episodes?.values?.flatten()?.firstOrNull()
                                                if (firstEp != null) {
                                                    onPlay(catalog.seriesUrl(firstEp.streamId), f.name)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Tab 1: Continuar Viendo
            if (continueWatchingList.isEmpty()) {
                EmptyStateView("No tienes contenido pendiente", "Las películas que empieces a ver aparecerán aquí con su progreso guardado.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 125.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(continueWatchingList) { item ->
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
}

@Composable
private fun TabPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Accent else Surface)
            .border(
                1.dp,
                if (isSelected) Accent else Color(0x33FFFFFF),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF041E19) else Color(0xFF94A3B8),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                color = if (isSelected) Color(0xFF041E19) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyStateView(title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = "Sin contenido",
                tint = Color(0xFF334155),
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
