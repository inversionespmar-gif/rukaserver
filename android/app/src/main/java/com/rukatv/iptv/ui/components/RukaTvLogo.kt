package com.rukatv.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet

/**
 * Logo RukaTV renderizado en texto con gradiente azul→violeta.
 * El "R" de Ruka tiene un estilo especial (negrita + cursiva) y "Tv" en color blanco.
 * Optionally shows the slogan below the logo text.
 */
@Composable
fun RukaTvLogo(
    modifier: Modifier = Modifier,
    size: TextUnit = 24.sp,
    showSlogan: Boolean = false,
    showIcon: Boolean = true
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(LogoBlue, LogoViolet)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showIcon) {
                // "R" icon box with gradient background
                LogoRIcon(size = (size.value * 1.4f).sp)
            }

            // "ukaTV" text with gradient brush on Ruka and white on Tv
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            brush = gradientBrush,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = size,
                            shadow = Shadow(
                                color = LogoBlue.copy(alpha = 0.4f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f
                            )
                        )
                    ) {
                        append("Ruka")
                    }
                    withStyle(
                        SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Light,
                            fontSize = size
                        )
                    ) {
                        append("Tv")
                    }
                }
            )
        }

        if (showSlogan) {
            Text(
                text = "Nueva generación en entretenimiento",
                color = Color(0xFF6B7A99),
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = if (showIcon) ((size.value * 1.4f) + 6f).dp else 0.dp)
            )
        }
    }
}

/**
 * Caja con la letra "R" estilizada y gradiente azul→violeta como icono del logo.
 */
@Composable
fun LogoRIcon(
    size: TextUnit = 32.sp,
    cornerRadius: Dp = 8.dp
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(LogoBlue, LogoViolet)
    )
    val iconSize = (size.value * 0.9f).dp

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(iconSize)
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                drawRect(brush = gradientBrush)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "R",
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = (size.value * 0.65f).sp,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(1f, 1f),
                    blurRadius = 3f
                )
            )
        )
    }
}
