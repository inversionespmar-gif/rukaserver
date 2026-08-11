package com.rukatv.iptv.player

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import com.rukatv.iptv.resolver.BestLeagueResolver
import com.rukatv.iptv.resolver.StreamInterceptor

@UnstableApi
class DrmPlayer(private val context: Context) {
    val view: SurfaceView = SurfaceView(context)
    private var exoPlayer: ExoPlayer? = null
    private val interceptor = StreamInterceptor(context)
    private val resolver = BestLeagueResolver()

    fun play(url: String) {
        try {
            stop()

            if (resolver.canResolve(url)) {
                Log.d(TAG, "Resolving via interceptor: $url")
                interceptor.intercept(url) { mpdUrl ->
                    if (mpdUrl != null) {
                        Log.d(TAG, "MPD capturado: $mpdUrl")
                        view.post {
                            try { playMpd(mpdUrl) } catch (e: Exception) {
                                Log.e(TAG, "playMpd error", e)
                            }
                        }
                    } else {
                        Log.w(TAG, "Interceptor fallo, intentando resolver directo")
                        resolveDirect(url)
                    }
                }
            } else if (url.endsWith(".mpd")) {
                view.post { playMpd(url) }
            } else {
                resolveDirect(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "play error", e)
        }
    }

    private fun resolveDirect(url: String) {
        Thread {
            try {
                val result = resolver.resolve(url)
                if (result != null) {
                    Log.d(TAG, "Directo OK: ${result.mpdUrl}")
                    view.post {
                        try { playMpd(result.mpdUrl) } catch (e: Exception) {
                            Log.e(TAG, "playMpd error", e)
                        }
                    }
                } else {
                    Log.e(TAG, "No se pudo resolver: $url")
                }
            } catch (e: Exception) {
                Log.e(TAG, "resolveDirect error", e)
            }
        }.apply { isDaemon = true; start() }
    }

    private fun playMpd(url: String) {
        try {
            exoPlayer?.release()
            exoPlayer = null

            val origin = try { Uri.parse(url).scheme + "://" + Uri.parse(url).host } catch (_: Exception) { "" }
            val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

            val dsFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(mapOf(
                    "User-Agent" to ua,
                    "Referer" to "$origin/",
                    "Origin" to origin,
                ))

            val drmSessionManager = try {
                val drmCallback = androidx.media3.exoplayer.drm.HttpMediaDrmCallback(
                    "https://201.235.66.122",
                    dsFactory
                )
                androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                    .setMultiSession(false)
                    .setPlayClearSamplesWithoutKeys(true)
                    .build(drmCallback)
            } catch (e: Exception) {
                Log.w(TAG, "DRM no disponible: ${e.message}")
                null
            }

            val chunkFactory = DefaultDashChunkSource.Factory(dsFactory)
            val dashFactory = DashMediaSource.Factory(chunkFactory, dsFactory)

            if (drmSessionManager != null) {
                dashFactory.setDrmSessionManagerProvider { drmSessionManager }
            }

            val mediaSource = dashFactory.createMediaSource(MediaItem.fromUri(Uri.parse(url)))

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 30000, 1500, 5000)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()
                .also { player ->
                    player.setMediaSource(mediaSource)
                    player.playWhenReady = true
                    player.prepare()

                    player.addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "Error: ${error.errorCodeName} - ${error.message}")
                        }
                        override fun onPlaybackStateChanged(state: Int) {
                            Log.d(TAG, "State: $state")
                        }
                    })

                    view.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            try { player.setVideoSurfaceView(view) } catch (_: Exception) {}
                        }
                        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, w: Int, h: Int) {}
                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
                    })

                    if (view.holder.surface?.isValid == true) {
                        player.setVideoSurfaceView(view)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "playMpd error", e)
        }
    }

    fun stop() {
        try {
            exoPlayer?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "stop error", e)
        }
    }

    fun release() {
        try {
            interceptor.destroy()
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "release error", e)
        }
    }

    companion object {
        private const val TAG = "DrmPlayer"
    }
}
