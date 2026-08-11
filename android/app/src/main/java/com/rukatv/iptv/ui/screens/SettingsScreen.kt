package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.GlassBorder
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    username: String = "Usuario Premium",
    host: String = "RukaTV Server",
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Configuración",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        // ── Profile Card ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            LogoBlue.copy(alpha = 0.15f),
                            LogoViolet.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(LogoBlue.copy(alpha = 0.5f), LogoViolet.copy(alpha = 0.4f))
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar with gradient ring
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(LogoBlue.copy(alpha = 0.3f), LogoViolet.copy(alpha = 0.3f)))
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(LogoBlue, LogoViolet)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Perfil",
                        tint = Accent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(username, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        // Premium badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(LogoBlue, LogoViolet))
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "PREMIUM",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(host, color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = "Perfil activo · Acceso completo",
                        color = Accent.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Video & Playback ──────────────────────────────────────────────────
        SettingsGroup(
            title = "Reproducción y Video",
            icon = Icons.Filled.Tv,
            iconColor = LogoBlue
        ) {
            SettingsRow(
                icon = Icons.Filled.Tv,
                title = "Calidad de video",
                subtitle = "Automática (4K / 1080p)",
                iconColor = LogoBlue
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Settings,
                title = "Reproductor de video",
                subtitle = "ExoPlayer / VLC (seleccionable)",
                iconColor = LogoViolet
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Language,
                title = "Audio y subtítulos",
                subtitle = "Español Latino por defecto",
                iconColor = Accent
            )
        }

        // ── Familia ───────────────────────────────────────────────────────────
        SettingsGroup(
            title = "Familia",
            icon = Icons.Filled.ChildCare,
            iconColor = Color(0xFF38A169)
        ) {
            SettingsRow(
                icon = Icons.Filled.ChildCare,
                title = "Control Parental",
                subtitle = "Perfiles limitados para toda la familia",
                iconColor = Color(0xFF38A169)
            )
        }

        // ── Descargas y Grabaciones ───────────────────────────────────────────
        SettingsGroup(
            title = "Descargas y Grabaciones",
            icon = Icons.Filled.Download,
            iconColor = Color(0xFFFFC107)
        ) {
            SettingsRow(
                icon = Icons.Filled.Download,
                title = "Descargas",
                subtitle = "Descarga tus contenidos favoritos para ver offline",
                iconColor = Color(0xFFFFC107)
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Videocam,
                title = "Grabaciones",
                subtitle = "Graba programas y reprodúcelos cuando quieras",
                iconColor = Color(0xFFFC8181)
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.SyncAlt,
                title = "Sincronización",
                subtitle = "Sincroniza preferencias en todos tus dispositivos",
                iconColor = LogoViolet
            )
        }

        // ── RUKA AI ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(LogoViolet.copy(alpha = 0.2f), LogoBlue.copy(alpha = 0.15f))
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(listOf(LogoViolet.copy(alpha = 0.6f), LogoBlue.copy(alpha = 0.5f))),
                    RoundedCornerShape(14.dp)
                )
                .clickable { }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(LogoViolet, LogoBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("RUKA AI", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(LogoViolet, LogoBlue)))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("NUEVO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        "Recomendaciones inteligentes, preferencias por voz y búsqueda",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // ── General ───────────────────────────────────────────────────────────
        SettingsGroup(title = "General", icon = Icons.Filled.Info, iconColor = TextSecondary) {
            SettingsRow(
                icon = Icons.Filled.Tv,
                title = "Modo Smart TV / D-Pad",
                subtitle = "Activado automáticamente según el dispositivo",
                iconColor = Accent
            )
            SettingsRowDivider()
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Acerca de RukaTV",
                subtitle = "Versión 2.0 Professional Stream",
                iconColor = TextSecondary
            )
        }

        // ── Logout button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x18EF4444))
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .clickable { onLogout() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color(0xFFFCA5A5),
                    modifier = Modifier.size(18.dp)
                )
                Text("Cerrar sesión", color = Color(0xFFFCA5A5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = Accent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 11.5.sp)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF2A3550),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(GlassBorder)
    )
}
