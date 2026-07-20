# libvlc Integration for Live TV Playback

**Date:** 2026-07-20
**Status:** Draft

## Problem

Some live TV channels fail to play (black screen → auto-pause) because ExoPlayer's built-in decoders cannot handle certain video or audio codecs used by upstream streams.

The Media3 `decoder-ffmpeg` extension was investigated but `ExperimentalFfmpegVideoRenderer` is **not functional** (marked "under development, not yet functional"). Only `FfmpegAudioRenderer` works, which would only fix audio codec issues. For video codec support, libvlc is required.

## Goal

Integrate libvlc (`org.videolan.android:libvlc-all`) as a replacement for ExoPlayer in `LiveTvScreen`, so live TV channels with exotic codecs are decoded by VLC's FFmpeg-based engine.

## Non-Goals

- No changes to `PlayerScreen` (movies/series) — remains on ExoPlayer.
- No UI changes to the channel list or overlay.
- No backend changes.

## Approach

### Architecture

Create a new `VlcPlayer` wrapper class that uses libvlc's native `LibVLC` + `MediaPlayer` API, rendering via a `SurfaceView`. `LiveTvScreen` instantiates `VlcPlayer` instead of `TvPlayer` for live stream playback.

```
Before: LiveTvScreen → TvPlayer (ExoPlayer) → PlayerView
After:  LiveTvScreen → VlcPlayer (libvlc) → SurfaceView
```

### Changes

#### 1. `android/app/build.gradle.kts`

Add libvlc dependency:

```kotlin
implementation("org.videolan.android:libvlc-all:3.6.5")
```

Using 3.6.x stable branch (4.0.x is EAP). Version 3.6.5 is the latest stable.

#### 2. `android/app/src/main/java/com/rukatv/iptv/player/VlcPlayer.kt`

New file — libvlc player wrapper with the same interface shape as `TvPlayer`:

```kotlin
package com.rukatv.iptv.player

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import android.net.Uri
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayer(context: Context) {
    private val libVlc: LibVLC = LibVLC(context, ArrayList())
    val mediaPlayer: MediaPlayer = MediaPlayer(libVlc)
    val view: SurfaceView = SurfaceView(context).also { sv ->
        mediaPlayer.setVideoSurfaceView(sv)
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
```

#### 3. `android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt`

Replace `TvPlayer` with `VlcPlayer`:

**Imports:**
```kotlin
// Remove: import com.rukatv.iptv.player.TvPlayer
// Remove: import com.rukatv.iptv.player.StreamKind
// Add:    import com.rukatv.iptv.player.VlcPlayer
```

**Player instantiation:**
```kotlin
// Before: val player = remember { TvPlayer(context) }
// After:  val player = remember { VlcPlayer(context) }
```

**Play a channel:**
```kotlin
// Before: player.prepare(catalog.liveUrl(ch.streamId), streamKindOf(ch.streamUrl))
// After:  player.play(ch.streamUrl)  // Use raw URL directly, skip backend proxy
```

**Cleanup:**
```kotlin
// Before: DisposableEffect(Unit) { onDispose { player.release() } }
// After:  DisposableEffect(Unit) { onDispose { player.stop(); player.release() } }
```

**AndroidView rendering (fullscreen and split view):**
```kotlin
// Before: fl.addView(playerView, ...)
// After:  fl.addView(player.view, ...)
```

#### 4. Play raw URL directly (skip backend proxy)

Live TV channels will play the `streamUrl` directly instead of going through `/live/.../id.m3u8`. This:
- Eliminates the backend proxy as a point of failure
- libvlc handles all protocol types natively (HTTP TS, HLS, etc.)
- No need for `streamKindOf()` detection

### Dependencies

| Library | Version | Source |
|---------|---------|--------|
| `org.videolan.android:libvlc-all` | 3.6.5 | Maven Central |

### APK Size Impact

Estimated: ~5 MB (native .so for arm64-v8a + armeabi-v7a). Acceptable for Android TV.

### Limitations

- libvlc 3.6.x does not support Android TV remote's D-pad seeking (rewind/fast-forward) the same way ExoPlayer does. The channel list overlay/selection still works via the remote.
- libvlc's `SurfaceView` has no built-in controller UI (unlike ExoPlayer's `PlayerView`). For live TV this is acceptable because the app's own controls (channel list overlay) replace the player controls.

## Testing

1. Build: `./gradlew app:compileDebugKotlin`
2. Install on Android TV: `./gradlew installDebug`
3. Verify channels that previously worked (first 4) — no regression
4. Verify channels that previously failed — confirm playback
5. Test VOD (movies/series) in PlayerScreen — still uses ExoPlayer, verify no regression
6. Test channel switching using the overlay list
7. Test fullscreen toggle, back button

## Rollback

1. Revert `LiveTvScreen.kt` to use `TvPlayer`
2. Remove `libvlc-all` dependency
3. Delete `VlcPlayer.kt`
