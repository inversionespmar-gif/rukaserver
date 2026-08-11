package com.rukatv.iptv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.GlassBorder
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet
import com.rukatv.iptv.ui.theme.SurfaceAlt
import com.rukatv.iptv.ui.theme.TextSecondary

data class NavItem(val key: String, val label: String, val icon: ImageVector)

@Composable
fun NavRail(
    items: List<NavItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomItems: List<NavItem> = emptyList()
) {
    // Expands when any item in the rail receives focus (D-pad navigation)
    var railFocused by remember { mutableStateOf(false) }
    val railWidth by animateDpAsState(
        targetValue = if (railFocused) 190.dp else 70.dp,
        animationSpec = tween(durationMillis = 260),
        label = "railWidth"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceAlt,
                        Color(0xFF0C1120)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
            )
            .onFocusChanged { railFocused = it.hasFocus }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        // ── Logo at top ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (railFocused) {
                RukaTvLogo(size = 16.sp, showIcon = true)
            } else {
                LogoRIcon(size = 28.sp, cornerRadius = 7.dp)
            }
        }

        // Separator
        RailDivider()

        // ── Main navigation items ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEach { item ->
                RailNavItem(
                    item = item,
                    isSelected = item.key == selected,
                    isExpanded = railFocused,
                    onSelect = onSelect
                )
            }
        }

        // ── Bottom items (settings / logout) ──────────────────────────────────
        if (bottomItems.isNotEmpty()) {
            RailDivider()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                bottomItems.forEach { item ->
                    RailNavItem(
                        item = item,
                        isSelected = item.key == selected,
                        isExpanded = railFocused,
                        onSelect = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun RailNavItem(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelect: (String) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    val isActive = isSelected || focused

    val bgBrush = when {
        isSelected -> Brush.horizontalGradient(
            colors = listOf(LogoBlue.copy(alpha = 0.18f), LogoViolet.copy(alpha = 0.12f))
        )
        focused -> Brush.horizontalGradient(
            colors = listOf(Color(0xFF1B2340), Color(0xFF131926))
        )
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (bgBrush != null) Modifier.background(bgBrush)
                else Modifier
            )
            .then(
                if (isSelected) Modifier.border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(listOf(LogoBlue.copy(alpha = 0.5f), LogoViolet.copy(alpha = 0.4f))),
                    shape = RoundedCornerShape(10.dp)
                ) else if (focused) Modifier.border(
                    width = 1.5.dp,
                    color = Accent.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onSelect(item.key) }
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leading accent bar for selected item
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isSelected) Brush.verticalGradient(listOf(LogoBlue, LogoViolet))
                        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
            )

            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = when {
                    isActive -> Accent
                    else -> Color(0xFF4A5878)
                },
                modifier = Modifier.size(20.dp)
            )

            // Label fades in when rail is expanded (on focus)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                Text(
                    text = item.label,
                    color = when {
                        isSelected -> Accent
                        focused -> Color(0xFFD1D9EB)
                        else -> TextSecondary
                    },
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        GlassBorder,
                        GlassBorder,
                        Color.Transparent
                    )
                )
            )
    )
}
