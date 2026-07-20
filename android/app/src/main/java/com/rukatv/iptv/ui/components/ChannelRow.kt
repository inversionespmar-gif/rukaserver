package com.rukatv.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Surface

@Composable
fun ChannelRow(
    index: Int,
    name: String,
    logo: String,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Accent.copy(alpha = 0.12f) else Surface)
            .border(
                width = if (focused) 1.5.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .focusable(interactionSource = interaction)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (focused) Accent else Color(0xFF1B2230))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                color = if (focused) Color(0xFF06231F) else Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Channel logo with dark background
        if (logo.isNotBlank()) {
            Box(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF181F2E))
            ) {
                AsyncImage(
                    model = logo,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Channel name
        Text(
            text = name,
            color = if (focused) Color(0xFFE8EEF7) else Color(0xFFCFD5E0),
            fontSize = 14.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (logo.isNotBlank()) 10.dp else 12.dp)
        )

        // Play arrow when focused
        if (focused) {
            Text(
                text = "▶",
                color = Accent,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
