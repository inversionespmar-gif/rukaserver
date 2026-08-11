package com.rukatv.iptv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.HeroGradientBottom
import com.rukatv.iptv.ui.theme.HeroGradientLeft
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet
import com.rukatv.iptv.ui.theme.StarGold
import kotlinx.coroutines.delay

@Composable
fun HeroCarousel(
    featuredMovies: List<VodStream>,
    modifier: Modifier = Modifier,
    onPlay: (VodStream) -> Unit,
    onDetails: (VodStream) -> Unit
) {
    if (featuredMovies.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-advance banner every 6 seconds
    LaunchedEffect(currentIndex, featuredMovies.size) {
        if (featuredMovies.size > 1) {
            delay(6000)
            currentIndex = (currentIndex + 1) % featuredMovies.size
        }
    }

    val currentMovie = featuredMovies[currentIndex]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFF060A12))
    ) {
        // Crossfade backdrop
        Crossfade(
            targetState = currentMovie,
            animationSpec = tween(700),
            label = "heroCrossfade"
        ) { movie ->
            Box(modifier = Modifier.fillMaxSize()) {
                val imageUrl = if (movie.backdrop.isNotBlank()) movie.backdrop else movie.poster
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E1420)))
                }

                // Bottom gradient
                Box(modifier = Modifier.fillMaxSize().background(HeroGradientBottom))
                // Left gradient
                Box(modifier = Modifier.fillMaxSize().background(HeroGradientLeft))
            }
        }

        // ── Content: bottom-left area ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            // Badges row: NEW, Episodio, Quality tags
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentMovie.isNewRelease) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(listOf(LogoBlue, LogoViolet))
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "ESTRENO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Year · Duration
                if (currentMovie.year.isNotBlank() || currentMovie.duration.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (currentMovie.year.isNotBlank()) append(currentMovie.year)
                            if (currentMovie.year.isNotBlank() && currentMovie.duration.isNotBlank()) append("  ·  ")
                            if (currentMovie.duration.isNotBlank()) append(currentMovie.duration)
                        },
                        color = Color(0xFFBBC8DF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quality badges
                BadgeTag("4K")
                BadgeTag("HDR")
                BadgeTag("DOLBY")
            }

            // Title
            Text(
                text = currentMovie.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                lineHeight = 32.sp,
                overflow = TextOverflow.Ellipsis
            )

            // Rating + Plot snippet
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (currentMovie.displayRating.isNotBlank() &&
                    currentMovie.displayRating != "0" &&
                    currentMovie.displayRating != "0.0"
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = StarGold,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = currentMovie.displayRating,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = currentMovie.plot.ifBlank { "Una experiencia cinematográfica imperdible disponible en HD y 4K." },
                    color = Color(0xFF8A95AA),
                    fontSize = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                HeroButton(
                    text = "Ver ahora",
                    icon = Icons.Filled.PlayArrow,
                    isPrimary = true,
                    onClick = { onPlay(currentMovie) }
                )
                HeroButton(
                    text = "Más info",
                    icon = Icons.Filled.Info,
                    isPrimary = false,
                    onClick = { onDetails(currentMovie) }
                )
            }
        }

        // ── Carousel dots (bottom-right) ──────────────────────────────────────
        if (featuredMovies.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                featuredMovies.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 6.dp,
                        animationSpec = tween(300),
                        label = "dotWidth_$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(LogoBlue, LogoViolet))
                                else
                                    Brush.horizontalGradient(listOf(Color(0x44FFFFFF), Color(0x44FFFFFF)))
                            )
                            .clickable { currentIndex = index }
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x44000000))
            .border(0.5.dp, Color(0x55FFFFFF), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFD1D9EB),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
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

    val bgModifier = if (isPrimary) {
        Modifier.background(
            brush = Brush.horizontalGradient(listOf(LogoBlue, LogoViolet)),
            shape = RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.background(
            color = Color(0x33FFFFFF),
            shape = RoundedCornerShape(10.dp)
        )
    }

    val borderColor by animateColorAsState(
        targetValue = if (focused) Accent else if (isPrimary) Color.Transparent else Color(0x44FFFFFF),
        animationSpec = tween(150),
        label = "heroBtnBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(bgModifier)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .shadow(if (focused) 10.dp else 0.dp, shape = RoundedCornerShape(10.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
