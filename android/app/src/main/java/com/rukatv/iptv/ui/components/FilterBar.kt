package com.rukatv.iptv.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Surface

enum class SortOption(val title: String) {
    RECENT("Más recientes"),
    POPULAR("Más populares"),
    RATING("Mejor valoradas"),
    TITLE_AZ("A - Z"),
    YEAR("Año")
}

data class FilterState(
    val selectedGenre: String = "Todo",
    val sortOption: SortOption = SortOption.RECENT,
    val selectedYear: String = "Todos",
    val selectedLanguage: String = "Todos",
    val selectedQuality: String = "Todas"
)

@Composable
fun FilterBar(
    filterState: FilterState,
    genres: List<String>,
    onGenreSelect: (String) -> Unit,
    onOpenFilterDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horizontal scrollable genre chips
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genres) { genre ->
                val isSelected = filterState.selectedGenre == genre
                FilterChip(
                    text = genre,
                    isSelected = isSelected,
                    onClick = { onGenreSelect(genre) }
                )
            }
        }

        // Advanced Filter Button
        Box(modifier = Modifier.padding(end = 14.dp)) {
            FilterIconButton(
                hasActiveFilters = filterState.selectedYear != "Todos" ||
                        filterState.selectedQuality != "Todas" ||
                        filterState.sortOption != SortOption.RECENT,
                onClick = onOpenFilterDialog
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    val bgColor = when {
        isSelected -> Accent
        focused -> Color(0xFF2D3748)
        else -> Color(0xFF1A202C)
    }

    val textColor = when {
        isSelected -> Color(0xFF041E19)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (focused) 2.dp else if (isSelected) 0.dp else 1.dp,
                color = if (focused) Accent else Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FilterIconButton(
    hasActiveFilters: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasActiveFilters) Accent.copy(alpha = 0.2f) else Surface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused || hasActiveFilters) Accent else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filtros",
                tint = if (hasActiveFilters) Accent else Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "Filtrar",
                color = if (hasActiveFilters) Accent else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    initialState: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit
) {
    var tempState by remember { mutableStateOf(initialState) }

    val years = listOf("Todos", "2026", "2025", "2024", "2023", "2022", "2021", "Clásicos")
    val languages = listOf("Todos", "Español", "Latino", "Inglés", "Japonés")
    val qualities = listOf("Todas", "4K", "1080p", "720p")

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filtros avanzados",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFFA0AEC0),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onDismiss() }
                    )
                }

                // Section 1: Ordenar por
                SectionTitle("Ordenar por")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SortOption.values().forEach { option ->
                        val selected = tempState.sortOption == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { tempState = tempState.copy(sortOption = option) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Accent else Color.Transparent)
                                    .border(1.5.dp, if (selected) Accent else Color(0xFF64748B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF041E19),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            Text(
                                option.title,
                                color = if (selected) Color.White else Color(0xFFCBD5E0),
                                fontSize = 13.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Section 2: Año
                SectionTitle("Año de estreno")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    years.forEach { y ->
                        SelectableChip(
                            text = y,
                            isSelected = tempState.selectedYear == y,
                            onClick = { tempState = tempState.copy(selectedYear = y) }
                        )
                    }
                }

                // Section 3: Idioma
                SectionTitle("Idioma")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { l ->
                        SelectableChip(
                            text = l,
                            isSelected = tempState.selectedLanguage == l,
                            onClick = { tempState = tempState.copy(selectedLanguage = l) }
                        )
                    }
                }

                // Section 4: Calidad
                SectionTitle("Calidad de video")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualities.forEach { q ->
                        SelectableChip(
                            text = q,
                            isSelected = tempState.selectedQuality == q,
                            onClick = { tempState = tempState.copy(selectedQuality = q) }
                        )
                    }
                }

                // Dialog Buttons Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Reset Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { tempState = FilterState() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Limpiar", color = Color(0xFFCBD5E0), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Apply Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Accent)
                            .clickable {
                                onApply(tempState)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aplicar filtros", color = Color(0xFF041E19), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF94A3B8),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Accent else Color(0xFF1E293B))
            .border(
                1.dp,
                if (isSelected) Accent else Color(0x22FFFFFF),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF041E19) else Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
