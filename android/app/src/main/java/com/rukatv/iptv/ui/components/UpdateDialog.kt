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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.data.repository.UpdateInfo
import com.rukatv.iptv.ui.theme.Accent

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    isDownloading: Boolean,
    downloadProgress: Float,
    onConfirmUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val confirmInteraction = remember { MutableInteractionSource() }
    val confirmFocused = confirmInteraction.collectIsFocusedAsState().value
    val cancelInteraction = remember { MutableInteractionSource() }
    val cancelFocused = cancelInteraction.collectIsFocusedAsState().value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC040714)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SystemUpdate,
                    contentDescription = "Actualización",
                    tint = Accent,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "Nueva Versión ${updateInfo.versionName} Disponible",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!updateInfo.releaseNotes.isNullOrEmpty()) {
                    Text(
                        text = updateInfo.releaseNotes ?: "",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isDownloading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            color = Accent,
                            trackColor = Color(0xFF334155),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Descargando actualización... ${(downloadProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Confirm button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (confirmFocused) Color.White else Accent)
                                .border(if (confirmFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                                .focusable(interactionSource = confirmInteraction)
                                .clickable(interactionSource = confirmInteraction, indication = null) {
                                    onConfirmUpdate()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Actualizar Ahora",
                                color = Color(0xFF041E19),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!updateInfo.forceUpdate) {
                            // Cancel button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (cancelFocused) Accent.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(if (cancelFocused) 2.dp else 1.dp, if (cancelFocused) Accent else Color(0xFF444444), RoundedCornerShape(8.dp))
                                    .focusable(interactionSource = cancelInteraction)
                                    .clickable(interactionSource = cancelInteraction, indication = null) {
                                        onDismiss()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Más Tarde",
                                    color = if (cancelFocused) Accent else Color(0xFFCCCCCC),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
