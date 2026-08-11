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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.EmbeddedMiniPlayer
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    movie: VodStream,
    allMovies: List<VodStream>,
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    progressStore: PlaybackProgressStore? = null,
    onBack: () -> Unit,
    onMovieClick: (VodStream) -> Unit,
    onPlay: (Long, String, String) -> Unit,
    onPlayAtPosition: (Long, String, String, Long) -> Unit = { id, title, poster, _ -> onPlay(id, title, poster) }
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val favKey = "movie:${movie.streamId}"
    val isFav = favorites.isFavorite(favSet, favKey)

    // Load saved progress for continue watching dialog
    var savedPositionMs by remember { mutableLongStateOf(0L) }
    var showContinueDialog by remember { mutableStateOf(false) }
    var miniPlayerPositionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(movie.streamId) {
        val url = catalog.movieUrl(movie.streamId)
        val pos = runCatching { progressStore?.getProgress(url)?.first() }.getOrNull() ?: 0L
        if (pos > 10_000L) {
            savedPositionMs = pos
            showContinueDialog = true
        }
    }

    // Fetch TMDB metadata
    var detailedMeta by remember { mutableStateOf<com.rukatv.iptv.data.remote.dto.VodDetailMeta?>(null) }
    LaunchedEffect(movie.streamId) {
        runCatching { catalog.vodInfo(movie.streamId) }.onSuccess { resp ->
            detailedMeta = resp.info
        }
    }

    val plot = remember(movie.streamId, detailedMeta) {
        detailedMeta?.plot?.takeIf { it.isNotBlank() }
            ?: movie.plot.takeIf { it.isNotBlank() }
            ?: "Disfruta de esta increíble producción con la mejor calidad en RukaTV."
    }
    val director = remember(detailedMeta) { detailedMeta?.director?.takeIf { it.isNotBlank() } }
    val cast = remember(detailedMeta) {
        detailedMeta?.cast?.takeIf { it.isNotBlank() }
            ?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() }?.take(5)
            ?.joinToString(", ")
    }
    val genres = remember(detailedMeta) {
        detailedMeta?.genre?.takeIf { it.isNotBlank() }
            ?.split(",", "/")?.map { it.trim() }?.filter { it.isNotBlank() }?.take(5)
            ?: emptyList()
    }
    val country = remember(detailedMeta) {
        detailedMeta?.releaseDate?.takeIf { it.isNotBlank() }?.take(4) ?: movie.year
    }
    val backdropUrl = remember(movie.streamId, detailedMeta) {
        detailedMeta?.coverBig?.takeIf { it.isNotBlank() }
            ?: detailedMeta?.movieImage?.takeIf { it.isNotBlank() }
            ?: movie.poster
    }

    val streamUrl = catalog.movieUrl(movie.streamId)

    // Determine start position for mini player
    val miniStartPos = if (showContinueDialog) 0L else savedPositionMs
    var miniPlayerResumePos by remember { mutableLongStateOf(miniStartPos) }

    val similarMovies = remember(movie.streamId) {
        allMovies.filter { it.streamId != movie.streamId }.take(12)
    }

    // Continue watching / start over dialog overlay
    if (showContinueDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xBB000000)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(380.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                    .padding(28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "¿Continuar viendo?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val minSec = savedPositionMs / 1000
                    Text(
                        "Parado en %d:%02d".format(minSec / 60, minSec % 60),
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Continue button
                        DetailActionChip(
                            text = "Continuar",
                            icon = Icons.Filled.PlayArrow,
                            isPrimary = true,
                            onClick = {
                                miniPlayerResumePos = savedPositionMs
                                showContinueDialog = false
                            }
                        )
                        // Start over button
                        DetailActionChip(
                            text = "Desde el inicio",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            isPrimary = false,
                            onClick = {
                                miniPlayerResumePos = 0L
                                showContinueDialog = false
                            }
                        )
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen blurred backdrop
        if (backdropUrl.isNotBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Dark gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xCC060A12), Color(0xF0060A12)),
                        radius = 1500f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Back button
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(CircleShape)
                        .background(Color(0x88000000))
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
                            modifier = Modifier.size(15.dp)
                        )
                        Text("Volver", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Main 2-column layout: Metadata LEFT + Mini Player RIGHT
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // ── LEFT COLUMN: Metadata ──────────────────────────────────
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title + Rating
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = movie.name,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // Rating badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = movie.displayRating,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Country | Year | Original name
                        Text(
                            text = buildString {
                                append(country)
                                if (movie.year.isNotBlank()) append(" | ${movie.year}")
                                if (movie.duration.isNotBlank() && movie.duration != "2h 14m") append(" | ${movie.duration}")
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )

                        // Genre chips
                        if (genres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Género:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                genres.forEach { genre ->
                                    GenreChip(genre)
                                }
                            }
                        }

                        // Director
                        if (!director.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Director:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(director, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                            }
                        }

                        // Cast
                        if (!cast.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Actores:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    cast,
                                    color = Color(0xFF00D4FF),
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Synopsis
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "Sinopsis:",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = plot,
                                color = Color(0xFFCBD5E0),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // ── RIGHT COLUMN: Mini Player ──────────────────────────────
                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EmbeddedMiniPlayer(
                            url = streamUrl,
                            startPositionMs = miniPlayerResumePos,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            onFullScreen = { currentPos ->
                                onPlayAtPosition(movie.streamId, movie.name, movie.poster, currentPos)
                            },
                            onPositionUpdate = { pos -> miniPlayerPositionMs = pos }
                        )
                    }
                }
            }

            // Action buttons row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Full screen
                    DetailActionChip(
                        text = "Pantalla completa",
                        icon = Icons.Filled.Fullscreen,
                        isPrimary = false,
                        onClick = {
                            onPlayAtPosition(movie.streamId, movie.name, movie.poster, miniPlayerPositionMs)
                        }
                    )
                    // CC / Subtitles (visual only for now)
                    DetailActionChip(
                        text = "Idioma",
                        icon = Icons.Filled.Subtitles,
                        isPrimary = false,
                        onClick = {}
                    )
                    // Favorite
                    DetailActionChip(
                        text = if (isFav) "Favorito" else "Favorito",
                        icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        isPrimary = isFav,
                        onClick = { scope.launch { favorites.toggle(favKey) } }
                    )
                }
            }

            // Suggestions
            if (similarMovies.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Quizás te guste",
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

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GenreChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = Color(0xFFE2E8F0), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun DetailActionChip(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val bg = when {
        focused -> Accent
        isPrimary -> Color(0x33FFFFFF)
        else -> Color(0x22FFFFFF)
    }
    val border = when {
        focused -> Accent
        isPrimary -> Accent.copy(alpha = 0.6f)
        else -> Color(0x44FFFFFF)
    }
    val tint = when {
        focused -> Color(0xFF060A12)
        isPrimary -> Accent
        else -> Color.White
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = text, tint = tint, modifier = Modifier.size(16.dp))
            Text(text, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
