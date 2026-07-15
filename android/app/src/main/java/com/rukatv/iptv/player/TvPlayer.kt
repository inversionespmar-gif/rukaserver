package com.rukatv.iptv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class TvPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build().apply {
        playWhenReady = true
    }

    /** Returns a new PlayerView bound to the shared ExoPlayer instance. */
    fun playerView(ctx: Context): PlayerView {
        return PlayerView(ctx).apply {
            player = this@TvPlayer.player
            useController = true
            controllerShowTimeoutMs = 3000
        }
    }

    fun prepare(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun release() { player.release() }
}
