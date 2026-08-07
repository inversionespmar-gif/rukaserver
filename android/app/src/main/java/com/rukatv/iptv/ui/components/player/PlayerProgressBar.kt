package com.rukatv.iptv.ui.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.PlayerAccent
import com.rukatv.iptv.ui.theme.PlayerSecondary

@Composable
fun PlayerProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle invalid duration (C.TIME_UNSET = Long.MIN_VALUE)
    val validDurationMs = if (durationMs > 0 && durationMs < Long.MAX_VALUE / 2) durationMs else 0L
    val currentSeconds = (currentPositionMs / 1000).coerceAtLeast(0)
    val durationSeconds = (validDurationMs / 1000).coerceAtLeast(0)
    val progress = if (validDurationMs > 0) (currentPositionMs.toFloat() / validDurationMs).coerceIn(0f, 1f) else 0f

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val displayProgress = if (isDragging) dragProgress else progress

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(if (isDragging) (dragProgress * validDurationMs).toLong() / 1000 else currentSeconds),
            color = PlayerSecondary,
            fontSize = 11.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .onSizeChanged { boxSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / boxSize.width).coerceIn(0f, 1f)
                        onSeek((newProgress * validDurationMs).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / boxSize.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgress * validDurationMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newProgress = (dragProgress + dragAmount / boxSize.width).coerceIn(0f, 1f)
                            dragProgress = newProgress
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF374151))
            )

            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PlayerAccent)
            )

            // Thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayProgress)
                    .height(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-8).dp)
                        .size(if (isDragging) 18.dp else 14.dp)
                        .clip(CircleShape)
                        .background(PlayerAccent)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

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
