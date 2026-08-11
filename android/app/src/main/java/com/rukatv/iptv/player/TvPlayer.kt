package com.rukatv.iptv.player

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.PlayerView
import com.rukatv.iptv.R

enum class StreamKind { AUTO, HLS, TS, PROGRESSIVE }

@UnstableApi
class TvPlayer(private val context: Context) {
    private var retryCount = 0
    private var lastPreparedUrl: String? = null
    private var lastPreparedKind: StreamKind = StreamKind.AUTO
    // Guard against re-entrant calls from within player listener callbacks
    private var isPreparing = false

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
        setUserAgent("Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
        setConnectTimeoutMs(15000)
        setReadTimeoutMs(25000)
        setAllowCrossProtocolRedirects(true)
    }

    private val renderersFactory = DefaultRenderersFactory(context).apply {
        setEnableDecoderFallback(true) // Enables software fallback if TV hardware decoder fails/crashes
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
    }

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpDataSourceFactory)

    // LoadControl tailored for IPTV & Smart TV low-RAM devices to prevent buffer underflow crashes & OOM
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15000, // Min buffer 15 seconds
            45000, // Max buffer 45 seconds
            2500,  // Min buffer before start
            5000   // Min buffer after rebuffer
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    // Called only when all retries have been exhausted (fatal error)
    var onFatalError: ((PlaybackException) -> Unit)? = null

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("TvPlayer", "Playback error [code=${error.errorCodeName}]: ${error.message}", error)
                    // Auto-retry up to 3 times on transient network or decoder glitches
                    // Use post() to avoid re-entrant calls into ExoPlayer from within its listener
                    if (retryCount < 3 && lastPreparedUrl != null && !isPreparing) {
                        retryCount++
                        val retryUrl = lastPreparedUrl!!
                        val retryKind = lastPreparedKind
                        val currentPos = runCatching { currentPosition }.getOrDefault(0L)
                        Log.w("TvPlayer", "Retrying playback (Attempt $retryCount/3) for: $retryUrl")
                        // Post to next looper cycle to avoid IllegalStateException from within listener
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            runCatching {
                                prepare(retryUrl, retryKind)
                                if (currentPos > 2000) {
                                    runCatching { seekTo(currentPos) }
                                }
                            }.onFailure { Log.e("TvPlayer", "Retry failed", it) }
                        }, 500L)
                    } else {
                        // All retries exhausted — notify UI with fatal error
                        Log.e("TvPlayer", "Max retries reached, giving up on: $lastPreparedUrl")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            onFatalError?.invoke(error)
                        }
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        retryCount = 0 // Reset retry count when playback starts successfully
                    }
                }
            })
        }

    /** Returns a new PlayerView bound to the shared ExoPlayer instance. */
    fun playerView(ctx: Context, onVisibilityChanged: ((Boolean) -> Unit)? = null): PlayerView {
        val appRes = ctx.resources
        val themedCtx = object : ContextWrapper(ctx) {
            private val theme = appRes.newTheme().apply {
                applyStyle(R.style.RukaPlayerController, true)
            }
            override fun getTheme() = theme
        }
        return PlayerView(themedCtx).apply {
            player = this@TvPlayer.player
            useController = false
            keepScreenOn = true
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    fun prepare(url: String, kind: StreamKind = StreamKind.AUTO) {
        if (url.isBlank()) return
        if (isPreparing) {
            Log.w("TvPlayer", "prepare() called while already preparing, ignoring: $url")
            return
        }
        Log.d("TvPlayer", "prepare: $url kind=$kind")
        lastPreparedUrl = url
        lastPreparedKind = kind
        isPreparing = true

        val mediaItem = MediaItem.fromUri(url)

        val resolvedKind = if (kind == StreamKind.AUTO) {
            val lower = url.lowercase()
            when {
                lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("output=hls") || lower.contains("m3u8") -> StreamKind.HLS
                lower.contains(".ts") || lower.contains("/ts/") || lower.endsWith("/ts") || lower.contains("output=ts") -> StreamKind.TS
                else -> StreamKind.PROGRESSIVE
            }
        } else kind

        val mediaSource: MediaSource = runCatching {
            when (resolvedKind) {
                StreamKind.HLS -> HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(mediaItem)
                StreamKind.TS -> ProgressiveMediaSource.Factory(httpDataSourceFactory, ExtractorsFactory { arrayOf(TsExtractor()) })
                    .createMediaSource(mediaItem)
                else -> mediaSourceFactory.createMediaSource(mediaItem)
            }
        }.getOrElse {
            mediaSourceFactory.createMediaSource(mediaItem)
        }

        try {
            player.stop()
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e("TvPlayer", "Error preparing media source: ${e.message}", e)
        } finally {
            isPreparing = false
        }
    }

    fun seekTo(positionMs: Long) {
        runCatching { player.seekTo(positionMs) }
    }

    fun setListener(listener: Player.Listener) {
        player.addListener(listener)
    }

    fun getSubtitleTracks(): List<SubtitleTrackInfo> {
        val tracks = runCatching { player.currentTracks }.getOrNull() ?: return emptyList()
        val result = mutableListOf<SubtitleTrackInfo>()
        var count = 1
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                val mediaGroup = group.mediaTrackGroup
                for (i in 0 until mediaGroup.length) {
                    val format = mediaGroup.getFormat(i)
                    val lang = format.language ?: ""
                    val label = format.label ?: if (lang.isNotBlank()) lang.uppercase() else "Subtítulo $count"
                    result.add(
                        SubtitleTrackInfo(
                            id = "${mediaGroup.id}_$i",
                            label = label,
                            language = lang,
                            isSelected = group.isTrackSelected(i),
                            trackGroup = mediaGroup,
                            trackIndex = i
                        )
                    )
                    count++
                }
            }
        }
        return result
    }

    fun selectSubtitleTrack(track: SubtitleTrackInfo?) {
        val currentParams = player.trackSelectionParameters
        val newParams = if (track == null) {
            currentParams.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
        } else if (track.trackGroup != null) {
            currentParams.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .addOverride(androidx.media3.common.TrackSelectionOverride(track.trackGroup, track.trackIndex))
                .build()
        } else currentParams

        player.trackSelectionParameters = newParams
    }

    fun getVideoQualities(): List<VideoQualityInfo> {
        val tracks = runCatching { player.currentTracks }.getOrNull() ?: return emptyList()
        val result = mutableListOf<VideoQualityInfo>()
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                val mediaGroup = group.mediaTrackGroup
                for (i in 0 until mediaGroup.length) {
                    val format = mediaGroup.getFormat(i)
                    val h = format.height
                    val label = if (h > 0) "${h}p" else "Calidad ${result.size + 1}"
                    result.add(
                        VideoQualityInfo(
                            id = "${mediaGroup.id}_$i",
                            label = label,
                            width = format.width,
                            height = format.height,
                            bitrate = format.bitrate,
                            isSelected = group.isTrackSelected(i),
                            trackGroup = mediaGroup,
                            trackIndex = i
                        )
                    )
                }
            }
        }
        return result
    }

    fun selectVideoQuality(qualityId: String) {
        val currentParams = player.trackSelectionParameters
        if (qualityId == "AUTO") {
            player.trackSelectionParameters = currentParams.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .build()
            return
        }
        val qualities = getVideoQualities()
        val match = qualities.find { it.id == qualityId || it.label == qualityId }
        if (match != null && match.trackGroup != null) {
            player.trackSelectionParameters = currentParams.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .addOverride(androidx.media3.common.TrackSelectionOverride(match.trackGroup, match.trackIndex))
                .build()
        }
    }

    fun release() {
        runCatching {
            player.stop()
            player.clearMediaItems()
            player.release()
        }.onFailure { Log.e("TvPlayer", "Error releasing player", it) }
    }
}

data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean,
    val trackGroup: androidx.media3.common.TrackGroup?,
    val trackIndex: Int
)

data class VideoQualityInfo(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val isSelected: Boolean,
    val trackGroup: androidx.media3.common.TrackGroup?,
    val trackIndex: Int
)

