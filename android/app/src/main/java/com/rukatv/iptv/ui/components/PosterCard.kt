package com.rukatv.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rukatv.iptv.ui.theme.Accent

@Composable
fun PosterCard(
    title: String,
    poster: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(if (focused) 2.dp else 0.dp, Accent, RoundedCornerShape(10.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        AsyncImage(
            model = poster,
            contentDescription = title,
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = title,
            color = Color(0xFFE8EEF7),
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(6.dp)
        )
    }
}
