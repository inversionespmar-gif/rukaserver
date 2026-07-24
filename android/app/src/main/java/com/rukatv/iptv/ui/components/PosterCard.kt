package com.rukatv.iptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun PosterCard(
    title: String,
    poster: String,
    modifier: Modifier = Modifier,
    width: Int = 0,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.07f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "posterScale"
    )

    Box(
        modifier = modifier
            .then(if (width > 0) Modifier.width(width.dp) else Modifier.fillMaxWidth())
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (focused) 12.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (focused) Accent.copy(alpha = 0.5f) else Color.Black,
                spotColor = if (focused) Accent.copy(alpha = 0.5f) else Color.Black
            )
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        // Poster image
        AsyncImage(
            model = poster,
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp),
            contentScale = ContentScale.Crop
        )
        // Gradient overlay — bottom darkens for legible title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.85f to Color(0xCC000000),
                            1.0f to Color(0xF0000000)
                        )
                    )
                )
        )
        // Title overlaid on the gradient
        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            lineHeight = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
