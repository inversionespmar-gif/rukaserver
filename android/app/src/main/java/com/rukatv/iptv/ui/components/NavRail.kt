package com.rukatv.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.SurfaceAlt

data class NavItem(val key: String, val label: String, val icon: String)

@Composable
fun NavRail(
    items: List<NavItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(170.dp)
            .background(SurfaceAlt)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val interaction = remember { MutableInteractionSource() }
            val focused = interaction.collectIsFocusedAsState().value
            val isSel = item.key == selected
            val color = if (isSel || focused) Accent else Color(0xFFE8EEF7)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) Color(0xFF1B2230) else Color.Transparent)
                    .border(
                        width = if (focused) 2.dp else 0.dp,
                        color = Accent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .focusable(interactionSource = interaction)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(item.key) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.icon, color = color, textAlign = TextAlign.Center)
                Text(item.label, color = color, textAlign = TextAlign.Center)
            }
        }
    }
}
