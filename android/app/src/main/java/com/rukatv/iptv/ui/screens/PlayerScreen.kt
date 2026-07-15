package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rukatv.iptv.player.TvPlayer

@Composable
fun PlayerScreen(url: String, title: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val player = remember { TvPlayer(context).apply { prepare(url) } }
    DisposableEffect(Unit) { onDispose { player.release() } }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable { onExit() }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { fl ->
                    fl.addView(player.playerView(ctx), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            }
        )
        androidx.compose.material3.Text(title, color = Color.White, modifier = Modifier.padding(12.dp))
    }
}
