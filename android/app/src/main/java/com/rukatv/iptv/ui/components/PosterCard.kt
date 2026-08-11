package com.rukatv.iptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet
import com.rukatv.iptv.ui.theme.StarGold

@Composable
fun PosterCard(
    title: String,
    poster: String,
    modifier: Modifier = Modifier,
    width: Int = 135,
    height: Int = 198,
    rating: String = "",
    quality: String = "",
    isNew: Boolean = false,
    year: String = "",
    progressFraction: Float = 0f,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.07f else 1.0f,
        animationSpec = tween(durationMillis = 190),
        label = "posterScale"
    )

    val cardShape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .then(if (width > 0) Modifier.width(width.dp) else Modifier.fillMaxWidth())
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (focused) 18.dp else 4.dp,
                shape = cardShape,
                ambientColor = if (focused) Accent.copy(alpha = 0.5f) else Color.Black,
                spotColor = if (focused) Accent.copy(alpha = 0.4f) else Color.Black
            )
            .clip(cardShape)
            .border(
                width = if (focused) 2.dp else 1.dp,
                brush = if (focused)
                    Brush.linearGradient(listOf(LogoBlue, LogoViolet))
                else
                    Brush.linearGradient(listOf(Color(0x18FFFFFF), Color(0x10FFFFFF))),
                shape = cardShape
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        // Main image container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
        ) {
            if (poster.isNotBlank()) {
                AsyncImage(
                    model = poster,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E1420)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = "Sin imagen",
                        tint = Color(0xFF2A3550),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x22000000),
                                0.45f to Color.Transparent,
                                0.72f to Color(0xBB060A12),
                                1.0f to Color(0xF2060A12)
                            )
                        )
                    )
            )

            // Top-left badge: NUEVO or quality
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isNew) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(listOf(LogoBlue, LogoViolet))
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NUEVO",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (quality.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xCC060A12))
                            .border(0.5.dp, Color(0x55FFFFFF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quality,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Top-right rating badge
            if (rating.isNotBlank() && rating != "0" && rating != "0.0") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xD9060A12))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = StarGold,
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = rating,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom content: Title + Year
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
                if (year.isNotBlank()) {
                    Text(
                        text = year,
                        color = Color(0xFF8A95AA),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Continue watching progress bar
            if (progressFraction > 0f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0x66000000))
                    ) {
                        // Fill with gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(LogoBlue, LogoViolet))
                                )
                        )
                    }
                }
            }
        }
    }
}
