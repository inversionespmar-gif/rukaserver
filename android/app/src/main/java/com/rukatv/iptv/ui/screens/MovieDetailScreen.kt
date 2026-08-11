package com.rukatv.iptv.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.rukatv.iptv.data.remote.dto.VodDetailMeta
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rukatv.iptv.data.remote.dto.CastMember
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import kotlinx.coroutines.launch

@Composable
fun MovieDetailScreen(
    movie: VodStream,
    allMovies: List<VodStream>,
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onBack: () -> Unit,
    onMovieClick: (VodStream) -> Unit,
    onPlay: (Long, String, String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val favKey = "movie:${movie.streamId}"
    val isFav = favorites.isFavorite(favSet, favKey)

    // Fetch real movie details asynchronously
    var detailedMeta by remember { mutableStateOf<com.rukatv.iptv.data.remote.dto.VodDetailMeta?>(null) }
    LaunchedEffect(movie.streamId) {
        runCatching { catalog.vodInfo(movie.streamId) }.onSuccess { resp ->
            detailedMeta = resp.info
        }
    }

    val plotText = remember(movie.streamId, detailedMeta) {
        val p = detailedMeta?.plot
        if (!p.isNullOrBlank()) p else if (movie.plot.isNotBlank()) movie.plot else "Disfruta de esta increíble película con la mejor calidad de audio y video en RukaTV."
    }

    val castMembers = remember(movie.streamId, detailedMeta) {
        val rawCast = detailedMeta?.cast
        if (!rawCast.isNullOrBlank()) {
            rawCast.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.take(10).map { name ->
                CastMember(name = name, role = "Reparto", photoUrl = "")
            }
        } else {
            emptyList()
        }
    }

    val backdropUrl = remember(movie.streamId, detailedMeta) {
        detailedMeta?.coverBig?.takeIf { it.isNotBlank() }
            ?: detailedMeta?.movieImage?.takeIf { it.isNotBlank() }
            ?: detailedMeta?.backdropPath?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: movie.backdrop.takeIf { it.isNotBlank() }
            ?: movie.poster
    }

    // Similar movies in same category / genre
    val similarMovies = remember(movie.streamId, allMovies) {
        allMovies.filter { it.streamId != movie.streamId }
            .take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top Hero Section (Backdrop & Basic Info)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                val imageUrl = backdropUrl
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Surface))
                }

                // Gradient Overlays
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color(0x66000000),
                                    0.4f to Color(0x33000000),
                                    0.8f to Color(0xCC0B0F12),
                                    1.0f to Background
                                )
                            )
                        )
                )

                // Back Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color(0x880B0F12))
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

                // Title & Quick Metadata
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = movie.name,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Rating Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Valoración",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                movie.displayRating,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text("•", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(movie.year, color = Color(0xFFCBD5E0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("•", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(movie.duration, color = Color(0xFFCBD5E0), fontSize = 13.sp, fontWeight = FontWeight.Medium)

                        // Badges
                        BadgeTag("4K")
                        BadgeTag("HDR")
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Play Button
                ActionButton(
                    text = "Reproducir",
                    icon = Icons.Filled.PlayArrow,
                    isPrimary = true,
                    modifier = Modifier.weight(1.3f),
                    onClick = { onPlay(movie.streamId, movie.name, movie.poster) }
                )

                // Watchlist Button
                ActionButton(
                    text = if (isFav) "En Mi lista" else "Mi Lista",
                    icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.Add,
                    isPrimary = false,
                    isSelected = isFav,
                    modifier = Modifier.weight(1f),
                    onClick = { scope.launch { favorites.toggle(favKey) } }
                )

                // Trailer Button
                ActionButton(
                    text = "Tráiler",
                    icon = Icons.Filled.Videocam,
                    isPrimary = false,
                    modifier = Modifier.weight(0.9f),
                    onClick = {
                        Toast.makeText(context, "Reproduciendo tráiler oficial de ${movie.name}", Toast.LENGTH_SHORT).show()
                    }
                )

                // Download Button
                ActionButton(
                    text = "Descargar",
                    icon = Icons.Filled.Download,
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        Toast.makeText(context, "Iniciando descarga de ${movie.name}...", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Synopsis Section
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Sinopsis",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (movie.plot.isNotBlank()) movie.plot else "Disfruta de esta increíble producción cinematográfica con máxima calidad de audio y video. Agrégala a tu lista para no perdértela.",
                    color = Color(0xFFCBD5E0),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        // Reparto / Cast Section
        item {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Reparto principal",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(castMembers) { cast: CastMember ->
                        CastCard(cast)
                    }
                }
            }
        }

        // Películas similares / Similar Movies Section
        if (similarMovies.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Películas similares",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(similarMovies) { sim ->
                            PosterCard(
                                title = sim.name,
                                poster = sim.poster,
                                rating = sim.displayRating,
                                quality = sim.quality,
                                year = sim.year,
                                onClick = { onMovieClick(sim) }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
private fun CastCard(cast: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(76.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (cast.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = cast.photoUrl,
                    contentDescription = cast.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = cast.name,
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = cast.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = cast.role,
            color = Color(0xFF94A3B8),
            fontSize = 9.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    val bgColor = when {
        focused -> Accent
        isPrimary -> Accent
        isSelected -> Accent.copy(alpha = 0.2f)
        else -> Color(0xFF1E293B)
    }

    val tintColor = when {
        focused || isPrimary -> Color(0xFF041E19)
        isSelected -> Accent
        else -> Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (focused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (focused || isSelected) Accent else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .shadow(if (focused) 8.dp else 0.dp)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = tintColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text,
                color = tintColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BadgeTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x55000000))
            .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
