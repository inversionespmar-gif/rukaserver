package com.rukatv.iptv.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.ui.components.RukaTvLogo
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.GlassBg
import com.rukatv.iptv.ui.theme.GlassBorder
import com.rukatv.iptv.ui.theme.LogoBlue
import com.rukatv.iptv.ui.theme.LogoViolet
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.theme.TextSecondary
import com.rukatv.iptv.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(vm: LoginViewModel, onLoggedIn: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loggedIn != null) { onLoggedIn(); return }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D1628),
                        Background
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background decorative glow spots
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(LogoBlue.copy(alpha = 0.07f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E1525).copy(alpha = 0.95f),
                            Color(0xFF080D18).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            LogoBlue.copy(alpha = 0.4f),
                            LogoViolet.copy(alpha = 0.3f),
                            GlassBorder
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 32.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            RukaTvLogo(size = 28.sp, showSlogan = true, showIcon = true)

            Spacer(Modifier.height(4.dp))

            // Form fields
            PremiumField(label = "Host / Servidor", value = state.host, onChange = { vm.setHost(it) })
            PremiumField(label = "Usuario", value = state.username, onChange = { vm.setUser(it) })
            PremiumField(label = "Contraseña", value = state.password, onChange = { vm.setPass(it) }, isPassword = true)

            // Error text
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = Color(0xFFFC8181),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(4.dp))

            // Login button with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (state.loading)
                            Brush.horizontalGradient(listOf(Color(0xFF1A2540), Color(0xFF1A2540)))
                        else
                            Brush.horizontalGradient(listOf(LogoBlue, LogoViolet))
                    )
                    .then(
                        if (!state.loading)
                            Modifier.focusable()
                        else Modifier
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { vm.login() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent
                    ),
                    elevation = null
                ) {
                    Text(
                        text = if (state.loading) "Conectando..." else "Iniciar sesión",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Footer
            Text(
                text = "RukaTV · Todo el entretenimiento en un solo lugar",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PremiumField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) LogoBlue.copy(alpha = 0.7f) else GlassBorder,
        animationSpec = tween(200),
        label = "fieldBorder_$label"
    )

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = label,
            color = if (isFocused) Accent else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Surface.copy(alpha = 0.6f))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = when (label) {
                        "Host / Servidor" -> "http://servidor.com:8080"
                        "Contraseña"      -> "••••••••"
                        else              -> label
                    },
                    color = Color(0xFF3A4560),
                    fontSize = 13.5.sp
                )
            }
            BasicTextField(
                value = if (isPassword && value.isNotEmpty()) "•".repeat(value.length) else value,
                onValueChange = { newVal ->
                    if (isPassword) {
                        // Allow deletion: count bullets
                        val bulletCount = newVal.count { it == '•' }
                        if (newVal.length < value.length) {
                            onChange(value.dropLast(value.length - newVal.length))
                        } else {
                            val newChars = newVal.filter { it != '•' }
                            onChange(value + newChars)
                        }
                    } else {
                        onChange(newVal)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                cursorBrush = SolidColor(Accent)
            )
        }
    }
}
