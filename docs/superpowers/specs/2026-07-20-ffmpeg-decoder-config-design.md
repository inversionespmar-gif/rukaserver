# FFmpeg Decoder Configuration for Live TV Playback

**Date:** 2026-07-20
**Status:** Draft

## Problem

Some live TV channels fail to play (black screen → auto-pause) because ExoPlayer's default decoders cannot handle certain codecs or container formats used by upstream streams.

The JellyFin `media3-ffmpeg-decoder` artifact is already declared in `app/build.gradle.kts` but is **not wired into `TvPlayer`**, so it has no effect.

## Goal

Configure `TvPlayer` to use FFmpeg-based video and audio decoders (`FfmpegVideoRenderer`, `FfmpegAudioRenderer`) via a custom `RenderersFactory`, so ExoPlayer can decode codecs its built-in decoders don't support (HEVC, AC3/E-AC3, AAC LATM, VP9, etc.).

## Non-Goals

- No UI changes
- No new dependencies (already declared)
- No backend changes
- No libvlc integration (reserved as a future step if FFmpeg is insufficient)

## Approach

### Architecture

ExoPlayer's `RenderersFactory` creates the audio/video decoders used for playback. The default factory creates platform decoders (MediaCodec). By passing a custom factory that includes `FfmpegVideoRenderer` and `FfmpegAudioRenderer`, FFmpeg decoders are used.

```
Before: ExoPlayer.Builder(context)
        → default RenderersFactory → MediaCodecVideoRenderer, MediaCodecAudioRenderer

After:  ExoPlayer.Builder(context, ffmpegRendererFactory)
        → FfmpegVideoRenderer, FfmpegAudioRenderer (via FFmpeg)
```

### Changes

#### 1. `android/app/src/main/java/com/rukatv/iptv/player/TvPlayer.kt`

Add FFmpeg renderers to the ExoPlayer builder:

```kotlin
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.video.VideoRendererEventListener
import org.jellyfin.media3.exoplayer.ffmpeg.FfmpegAudioRenderer
import org.jellyfin.media3.exoplayer.ffmpeg.FfmpegVideoRenderer

// In init block:
val renderersFactory = RenderersFactory { handler, videoListener, audioListener, textOutput, metadataOutput ->
    arrayOf(
        FfmpegVideoRenderer(handler, videoListener, FfmpegVideoRenderer.ALLOW_EXTENSION_ANDROID),
        FfmpegAudioRenderer(handler, audioListener),
        // Keep existing text/metadata renderers...
    )
}

player = ExoPlayer.Builder(context, renderersFactory)
    .build()
```

This replaces the default `RenderersFactory` with one that:
- Uses `FfmpegVideoRenderer` for video (with `ALLOW_EXTENSION_ANDROID` to prefer hardware decoder, falling back to FFmpeg)
- Uses `FfmpegAudioRenderer` for audio
- Media3's default text/metadata renderers are handled by the framework

#### 2. `android/app/build.gradle.kts`

Remove the old `media3-ffmpeg-decoder` dependency if needed, OR keep it if there are no conflicts. Actually, the JellyFin artifact IS the FFmpeg decoder we need.

### Dependencies

| Library | Version | Status |
|---------|---------|--------|
| `org.jellyfin.media3:media3-ffmpeg-decoder` | 1.3.1+1 | Already declared |

No new dependencies needed.

### Fallback Behavior

If FFmpeg decoder cannot handle a stream, ExoPlayer will throw a `PlaybackException`. The app already logs this in `TvPlayer.kt`:
```kotlin
player.addListener(object : Player.Listener {
    override fun onPlayerError(e: PlaybackException) {
        Log.e("TvPlayer", "Playback error", e)
    }
})
```

A future improvement could add automatic fallback to libvlc on error, but is out of scope.

## Testing

1. Build: `./gradlew app:compileDebugKotlin`
2. Install on device: `./gradlew installDebug`
3. Verify channels that previously worked (first 4) — no regression
4. Verify channels that previously failed — confirm playback
5. Test VOD (movies/series) — verify no regression
6. Test seek, pause, resume on live TV

## Out of Scope

- libvlc integration
- Runtime player switching
- Backend proxy changes
