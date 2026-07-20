package com.rukatv.iptv.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayer(context: Context) {
    private val libVlc: LibVLC = LibVLC(context, ArrayList())
    val mediaPlayer: MediaPlayer = MediaPlayer(libVlc)
    val view: SurfaceView = SurfaceView(context).also { sv ->
        mediaPlayer.vlcVout.setVideoView(sv)
        mediaPlayer.vlcVout.attachViews()
    }

    fun play(url: String) {
        val media = Media(libVlc, Uri.parse(url))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=3000")
        mediaPlayer.media = media
        mediaPlayer.play()
    }

    fun stop() {
        mediaPlayer.stop()
    }

    fun release() {
        mediaPlayer.release()
        libVlc.release()
    }
}
