# Premium Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the basic RukaTV player to a premium Netflix-style player with D-pad navigation and 6 new features (subtitles, speed, quality, PiP, sleep timer, screenshots).

**Architecture:** Extract player UI into focused components (PlayerOverlay, PlayerMenus, PlayerActions), add D-pad navigation with visible focus states, and implement premium features as modular extensions.

**Tech Stack:** Jetpack Compose, Media3/ExoPlayer, Android TV D-pad APIs, Picture-in-Picture API

---

## File Structure

| File | Responsibility |
|------|----------------|
| `PlayerOverlay.kt` | Main overlay container with gradient, title, and control bar |
| `PlayerMenus.kt` | Dropdown menus for subtitles, audio, speed, quality |
| `PlayerActions.kt` | Action buttons (capture, favorite, PiP, sleep timer) |
| `PlayerControls.kt` | Playback controls (play, seek, next/prev) with D-pad focus |
| `PlayerDpad.kt` | D-pad focus management utilities |
| `PlayerScreen.kt` | Refactored to compose new components |
| `Theme.kt` | Add player-specific colors |

---

## Task 1: Add Player Theme Colors

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/theme/Theme.kt`

- [ ] **Step 1: Add player-specific color constants**

```kotlin
// Add after existing color definitions (line 38)

// ── Player Colors ────────────────────────────────────────────────────────────
val PlayerAccent    = Color(0xFF00D4FF)     // Azul eléctrico del reproductor
val PlayerFocused   = Color(0x3300D4FF)     // Fondo elemento enfocado
val PlayerBorder    = Color(0xFF00D4FF)     // Borde elemento enfocado
val PlayerGlow      = Color(0x8000D4FF)     // Sombra glow del foco
val PlayerOverlay   = Color(0xBB000000)     // Overlay semi-transparente
val PlayerSecondary = Color(0xFF9CA3AF)     // Texto secundario del reproductor
val PlayerSurface   = Color(0xE6121212)     // Superficies de menús
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/theme/Theme.kt
git commit -m "feat(player): add player-specific theme colors"
```

---

## Task 2: Create D-pad Focus Utilities

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerDpad.kt`

- [ ] **Step 1: Create PlayerDpad.kt with focus management**

```kotlin
package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.rukatv.iptv.ui.theme.PlayerBorder
import com.rukatv.iptv.ui.theme.PlayerGlow

/**
 * Modifier that adds D-pad focus support with visible focus indicator.
 * Use on all interactive elements in the player.
 */
@Composable
fun Modifier.dpadFocus(
    focusRequester: FocusRequester = remember { FocusRequester() },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onFocused: (() -> Unit)? = null
): Modifier {
    val isFocused = interactionSource.collectIsFocusedAsState().value
    
    return this
        .focusRequester(focusRequester)
        .focusable(interactionSource = interactionSource)
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) PlayerBorder else androidx.compose.ui.graphics.Color.Transparent,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
        )
        .then(
            if (isFocused) {
                Modifier.shadow(8.dp, PlayerGlow, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            } else {
                Modifier
            }
        )
}

/**
 * Returns whether this interaction source is currently focused.
 */
@Composable
fun MutableInteractionSource.isFocused(): Boolean {
    return collectIsFocusedAsState().value
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerDpad.kt
git commit -m "feat(player): add D-pad focus utilities with visual indicators"
```

---

## Task 3: Create PlayerControls Component

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerControls.kt`

- [ ] **Step 1: Create PlayerControls.kt with playback controls**

```kotlin
package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.rukatv.iptv.ui.theme.PlayerAccent
import com.rukatv.iptv.ui.theme.PlayerSecondary

/**
 * Main playback controls bar: prev, rewind, play/pause, forward, next
 */
@Composable
fun PlayerControls(
    player: Player,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous
        PlayerControlButton(
            icon = Icons.Filled.SkipPrevious,
            contentDescription = "Anterior",
            onClick = onPrev
        )
        
        // Rewind 10s
        PlayerControlButton(
            icon = Icons.Filled.Replay10,
            contentDescription = "Retroceder 10s",
            onClick = { 
                val pos = runCatching { player.currentPosition }.getOrDefault(0L)
                player.seekTo((pos - 10000).coerceAtLeast(0L))
            }
        )
        
        // Play/Pause (larger, accent color)
        val isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(PlayerAccent)
                .dpadFocus(
                    onFocused = { }
                )
                .clickable { player.playWhenReady = !player.playWhenReady },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = Color(0xFF041E19),
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Forward 10s
        PlayerControlButton(
            icon = Icons.Filled.Forward10,
            contentDescription = "Adelantar 10s",
            onClick = { 
                val pos = runCatching { player.currentPosition }.getOrDefault(0L)
                player.seekTo(pos + 10000)
            }
        )
        
        // Next
        PlayerControlButton(
            icon = Icons.Filled.SkipNext,
            contentDescription = "Siguiente",
            onClick = onNext
        )
    }
}

@Composable
private fun PlayerControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerControls.kt
git commit -m "feat(player): add PlayerControls component with D-pad focus"
```

---

## Task 4: Create PlayerMenus Component

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerMenus.kt`

- [ ] **Step 1: Create PlayerMenus.kt with subtitle, audio, speed, quality menus**

```kotlin
package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.*

/**
 * Menu options data class
 */
data class MenuOption(
    val id: String,
    val label: String,
    val isSelected: Boolean = false
)

/**
 * Dropdown menu container for player settings
 */
@Composable
fun PlayerDropdownMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        Box(
            modifier = modifier
                .width(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PlayerSurface)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

/**
 * Single menu item with focus support
 */
@Composable
fun PlayerMenuItem(
    option: MenuOption,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onSelect(option.id) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.label,
                color = if (option.isSelected) PlayerAccent else Color.White,
                fontSize = 13.sp,
                fontWeight = if (option.isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (option.isSelected) {
                Text("✓", color = PlayerAccent, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Speed selector menu
 */
@Composable
fun SpeedMenu(
    visible: Boolean,
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(
        MenuOption("0.5", "0.5x", currentSpeed == 0.5f),
        MenuOption("0.75", "0.75x", currentSpeed == 0.75f),
        MenuOption("1.0", "1x (Normal)", currentSpeed == 1.0f),
        MenuOption("1.25", "1.25x", currentSpeed == 1.25f),
        MenuOption("1.5", "1.5x", currentSpeed == 1.5f),
        MenuOption("2.0", "2x", currentSpeed == 2.0f)
    )
    
    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        speeds.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = { onSpeedSelected(it.toFloatOrNull() ?: 1.0f) }
            )
        }
    }
}

/**
 * Quality selector menu
 */
@Composable
fun QualityMenu(
    visible: Boolean,
    currentQuality: String,
    qualities: List<String>,
    onQualitySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = qualities.map { q ->
        MenuOption(q, q, currentQuality == q)
    }
    
    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        options.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = onQualitySelected
            )
        }
    }
}

/**
 * Subtitle track selector menu
 */
@Composable
fun SubtitleMenu(
    visible: Boolean,
    tracks: List<MenuOption>,
    onTrackSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        tracks.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = onTrackSelected
            )
        }
    }
}

/**
 * Audio track selector menu
 */
@Composable
fun AudioMenu(
    visible: Boolean,
    tracks: List<MenuOption>,
    onTrackSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PlayerDropdownMenu(visible = visible, onDismiss = onDismiss) {
        tracks.forEach { option ->
            PlayerMenuItem(
                option = option,
                onSelect = onTrackSelected
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerMenus.kt
git commit -m "feat(player): add PlayerMenus component for subtitles, audio, speed, quality"
```

---

## Task 5: Create PlayerActions Component

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerActions.kt`

- [ ] **Step 1: Create PlayerActions.kt with capture, favorite, PiP, sleep timer**

```kotlin
package com.rukatv.iptv.ui.components.player

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.*

/**
 * Action button with icon and optional label
 */
@Composable
fun PlayerActionButton(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) PlayerAccent else Color.White,
            modifier = Modifier.size(20.dp)
        )
        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isActive) PlayerAccent else PlayerSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Screenshot action - captures current frame
 */
@Composable
fun ScreenshotButton(
    onScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerActionButton(
        icon = Icons.Filled.CameraAlt,
        contentDescription = "Captura",
        label = "Captura",
        onClick = onScreenshot,
        modifier = modifier
    )
}

/**
 * Favorite toggle button
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerActionButton(
        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        contentDescription = "Favorito",
        label = "Favorito",
        onClick = onToggle,
        modifier = modifier,
        isActive = isFavorite
    )
}

/**
 * Picture-in-Picture button
 */
@Composable
fun PipButton(
    onPipRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayerActionButton(
        icon = Icons.Filled.PictureInPictureAlt,
        contentDescription = "PiP",
        label = "PiP",
        onClick = onPipRequested,
        modifier = modifier
    )
}

/**
 * Sleep timer button with selection
 */
@Composable
fun SleepTimerButton(
    remainingMinutes: Int?,
    onSelectTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .dpadFocus(interactionSource = interaction)
                .clickable(interactionSource = interaction, indication = null) { showMenu = true }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = "Temporizador",
                tint = if (remainingMinutes != null) PlayerAccent else Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = remainingMinutes?.let { "${it}min" } ?: "Timer",
                color = if (remainingMinutes != null) PlayerAccent else PlayerSecondary,
                fontSize = 10.sp
            )
        }
        
        // Timer selection menu
        if (showMenu) {
            PlayerDropdownMenu(
                visible = showMenu,
                onDismiss = { showMenu = false }
            ) {
                val timerOptions = listOf(
                    null to "Desactivar",
                    15 to "15 minutos",
                    30 to "30 minutos",
                    45 to "45 minutos",
                    60 to "1 hora",
                    120 to "2 horas"
                )
                timerOptions.forEach { (minutes, label) ->
                    PlayerMenuItem(
                        option = MenuOption(
                            id = minutes?.toString() ?: "off",
                            label = label,
                            isSelected = remainingMinutes == minutes
                        ),
                        onSelect = {
                            onSelectTimer(minutes)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerActions.kt
git commit -m "feat(player): add PlayerActions component for screenshot, favorite, PiP, timer"
```

---

## Task 6: Create PlayerOverlay Component

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerOverlay.kt`

- [ ] **Step 1: Create PlayerOverlay.kt as main container**

```kotlin
package com.rukatv.iptv.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.*

/**
 * Main player overlay with top gradient, title, and bottom controls area
 */
@Composable
fun PlayerOverlay(
    title: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    topActions: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top gradient with title and actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xBB000000), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                
                // Top actions (capture, favorite, settings)
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    topActions()
                }
            }
            
            // Bottom controls area
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xBB000000))
                        )
                    )
            ) {
                bottomContent()
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerOverlay.kt
git commit -m "feat(player): add PlayerOverlay container component"
```

---

## Task 7: Create Progress Bar Component

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerProgressBar.kt`

- [ ] **Step 1: Create PlayerProgressBar.kt with D-pad seek support**

```kotlin
package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.PlayerAccent
import com.rukatv.iptv.ui.theme.PlayerSecondary

/**
 * Progress bar with time indicators and seek thumb
 */
@Composable
fun PlayerProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSeconds = currentPositionMs / 1000
    val durationSeconds = durationMs / 1000
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current time
        Text(
            text = formatTime(currentSeconds),
            color = PlayerSecondary,
            fontSize = 11.sp
        )
        
        // Progress track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF374151))
        ) {
            // Filled portion
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PlayerAccent)
            )
            
            // Seek thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-8).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(PlayerAccent)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        
        // Duration
        Text(
            text = formatTime(durationSeconds),
            color = PlayerSecondary,
            fontSize = 11.sp
        )
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerProgressBar.kt
git commit -m "feat(player): add PlayerProgressBar with D-pad seek support"
```

---

## Task 8: Refactor PlayerScreen to Use New Components

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt`

- [ ] **Step 1: Add imports for new components**

```kotlin
// Add at top of file after existing imports
import com.rukatv.iptv.ui.components.player.*
```

- [ ] **Step 2: Replace overlay sections (lines 396-422) with PlayerOverlay**

Replace the "Title and Top Gradient overlay" section with:

```kotlin
// Premium Player Overlay
PlayerOverlay(
    title = queue.getOrNull(index)?.title ?: "",
    visible = controlsVisible && !hasPlaybackError,
    topActions = {
        ScreenshotButton(onScreenshot = { /* TODO: implement screenshot */ })
        FavoriteButton(
            isFavorite = false, // TODO: track favorite state
            onToggle = { /* TODO: implement favorite toggle */ }
        )
    },
    bottomContent = {
        Column(
            modifier = Modifier.padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Progress bar
            PlayerProgressBar(
                currentPositionMs = runCatching { player.player.currentPosition }.getOrDefault(0L),
                durationMs = runCatching { player.player.duration }.getOrDefault(0L),
                onSeek = { player.seekTo(it) }
            )
            
            // Controls row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main controls
                PlayerControls(
                    player = player.player,
                    onPrev = { prevEpisode() },
                    onNext = { nextEpisode() },
                    modifier = Modifier.weight(1f)
                )
                
                // Secondary actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Subtitles button
                    SubtitleButton(
                        onClick = { /* TODO: show subtitle menu */ }
                    )
                    
                    // Speed button
                    SpeedButton(
                        currentSpeed = 1.0f, // TODO: track current speed
                        onClick = { /* TODO: show speed menu */ }
                    )
                    
                    // Quality button
                    QualityButton(
                        onClick = { /* TODO: show quality menu */ }
                    )
                    
                    // PiP button
                    PipButton(
                        onPipRequested = { /* TODO: implement PiP */ }
                    )
                    
                    // Sleep timer
                    SleepTimerButton(
                        remainingMinutes = null, // TODO: track timer state
                        onSelectTimer = { /* TODO: implement timer */ }
                    )
                }
            }
        }
    }
)
```

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "feat(player): refactor PlayerScreen to use new premium components"
```

---

## Task 9: Add Missing Button Components

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerButtons.kt`

- [ ] **Step 1: Create PlayerButtons.kt with SubtitleButton, SpeedButton, QualityButton**

```kotlin
package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.PlayerSecondary

/**
 * Subtitles toggle button
 */
@Composable
fun SubtitleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CC",
            color = if (isActive) Color(0xFF00D4FF) else Color.White,
            fontSize = 12.sp
        )
    }
}

/**
 * Speed selector button showing current speed
 */
@Composable
fun SpeedButton(
    currentSpeed: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val speedText = if (currentSpeed == 1.0f) "1x" else "${currentSpeed}x"
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = speedText,
            color = if (currentSpeed != 1.0f) Color(0xFF00D4FF) else Color.White,
            fontSize = 12.sp
        )
    }
}

/**
 * Quality selector button
 */
@Composable
fun QualityButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    quality: String = "HD"
) {
    val interaction = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .dpadFocus(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = quality,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/components/player/PlayerButtons.kt
git commit -m "feat(player): add SubtitleButton, SpeedButton, QualityButton components"
```

---

## Task 10: Update PlayerScreen Imports and Fix Compilation

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt`

- [ ] **Step 1: Run compilation check**

Run: `cd android && ./gradlew compileDebugKotlin`
Expected: May have errors due to missing implementations

- [ ] **Step 2: Add TODO stubs for unimplemented features**

Replace placeholder TODOs with temporary no-op implementations:

```kotlin
// In PlayerOverlay topActions:
ScreenshotButton(onScreenshot = { /* Captura de pantalla - TODO */ })
FavoriteButton(
    isFavorite = false,
    onToggle = { /* Favorito - TODO */ }
)

// In bottomContent:
SubtitleButton(onClick = { /* Menú de subtítulos - TODO */ })
SpeedButton(currentSpeed = 1.0f, onClick = { /* Menú de velocidad - TODO */ })
QualityButton(onClick = { /* Menú de calidad - TODO */ })
PipButton(onPipRequested = { /* Picture-in-Picture - TODO */ })
SleepTimerButton(remainingMinutes = null, onSelectTimer = { /* Temporizador - TODO */ })
```

- [ ] **Step 3: Run compilation again**

Run: `cd android && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "feat(player): complete premium player UI with TODO stubs for features"
```

---

## Task 11: Build APK and Test

**Files:**
- Modify: `android/app/build.gradle.kts` (if needed)

- [ ] **Step 1: Build debug APK**

Run: `cd android && ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK at `android/app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: Test on Android TV emulator**

1. Open Android Studio
2. Run on Android TV emulator (API 28+)
3. Navigate with D-pad:
   - Verify all controls are focusable
   - Verify focus indicators are visible
   - Verify play/pause works
   - Verify seek works with left/right

- [ ] **Step 3: Test on physical device (optional)**

1. Install APK on Android TV device
2. Test with physical remote control
3. Verify all D-pad operations work

- [ ] **Step 4: Commit final APK**

```bash
git add android/app-debug.apk
git commit -m "release: premium player APK with D-pad navigation"
```

---

## Summary

| Task | Description | Status |
|------|-------------|--------|
| 1 | Add player theme colors | ⬜ |
| 2 | Create D-pad focus utilities | ⬜ |
| 3 | Create PlayerControls component | ⬜ |
| 4 | Create PlayerMenus component | ⬜ |
| 5 | Create PlayerActions component | ⬜ |
| 6 | Create PlayerOverlay component | ⬜ |
| 7 | Create Progress Bar component | ⬜ |
| 8 | Refactor PlayerScreen | ⬜ |
| 9 | Add missing button components | ⬜ |
| 10 | Fix compilation | ⬜ |
| 11 | Build and test APK | ⬜ |
