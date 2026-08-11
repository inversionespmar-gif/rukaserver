package com.rukatv.iptv.player

import android.content.Context
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayer(context: Context) {
    private val libVlc: LibVLC = LibVLC(context, ArrayList())
    val mediaPlayer: MediaPlayer = MediaPlayer(libVlc)
    val view: SurfaceView = SurfaceView(context)
    private var pendingUrl: String? = null

    init {
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.vlcVout.setVideoSurface(holder.surface, holder)
                mediaPlayer.vlcVout.attachViews()
                pendingUrl?.let { url ->
                    pendingUrl = null
                    playInternal(url)
                }
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                if (w > 0 && h > 0) {
                    mediaPlayer.vlcVout.setWindowSize(w, h)
                    view.requestLayout()
                }
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mediaPlayer.vlcVout.detachViews()
            }
        })

        mediaPlayer.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Vout -> {
                    view.post { view.requestLayout() }
                }
                MediaPlayer.Event.Playing -> {
                    view.post { view.requestLayout() }
                }
                MediaPlayer.Event.EncounteredError -> {
                    android.util.Log.e("VlcPlayer", "Stream error encountered")
                }
            }
        }
    }

    fun play(url: String) {
        if (view.holder.surface?.isValid == true) {
            playInternal(url)
        } else {
            pendingUrl = url
        }
    }

    private fun playInternal(url: String) {
        try {
            mediaPlayer.stop()
            val media = Media(libVlc, Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=3000")
            media.addOption(":no-video-title-show")
            mediaPlayer.media = media
            mediaPlayer.play()
            view.post {
                view.requestLayout()
            }
        } catch (e: Exception) {
            android.util.Log.e("VlcPlayer", "playInternal error: ${e.message}")
        }
    }

    fun stop() {
        try {
            pendingUrl = null
            mediaPlayer.stop()
        } catch (e: Exception) {
            android.util.Log.e("VlcPlayer", "stop error: ${e.message}")
        }
    }

    fun release() {
        try {
            pendingUrl = null
            mediaPlayer.release()
            libVlc.release()
        } catch (e: Exception) {
            android.util.Log.e("VlcPlayer", "release error: ${e.message}")
        }
    }
}
