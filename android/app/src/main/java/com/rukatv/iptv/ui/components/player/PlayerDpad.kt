package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isFocused = interactionSource.collectIsFocusedAsState().value

    return this
        .focusRequester(focusRequester)
        .focusable(interactionSource = interactionSource)
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) PlayerBorder else Color.Transparent,
            shape = RoundedCornerShape(4.dp)
        )
        .then(
            if (isFocused) {
                Modifier.shadow(8.dp, PlayerGlow, shape = RoundedCornerShape(4.dp))
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
