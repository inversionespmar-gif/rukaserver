# libvlc Integration for Live TV Playback — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace ExoPlayer with libvlc in LiveTvScreen so channels with unsupported codecs play correctly.

**Architecture:** New `VlcPlayer` class wraps libvlc native API (`LibVLC` + `MediaPlayer`) with `SurfaceView` output. `LiveTvScreen` uses `VlcPlayer` instead of `TvPlayer`, playing the raw `streamUrl` directly. PlayerScreen (VOD) stays on ExoPlayer.

**Tech Stack:** Kotlin, Jetpack Compose, libvlc 3.6.5, AndroidX Media3 (unaffected), View interop via `AndroidView`

---

### Task 1: Add libvlc dependency

**Files:**
- Modify: `android/app/build.gradle.kts:53-58`

- [ ] **Step 1: Add libvlc-all dependency**

Insert after the `media3` dependencies block:

```kotlin
implementation("org.videolan.android:libvlc-all:3.6.5")
```

- [ ] **Step 2: Build check**

Run: `cd android && ./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (libvlc resolves and compiles)

- [ ] **Step 3: Commit**

```bash
git add android/app/build.gradle.kts
git commit -m "chore(deps): add libvlc-all 3.6.5"
```

---

### Task 2: Create VlcPlayer wrapper

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/player/VlcPlayer.kt`

- [ ] **Step 1: Write VlcPlayer.kt**

```kotlin
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

- [ ] **Step 2: Build check**

Run: `cd android && ./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/player/VlcPlayer.kt
git commit -m "feat(player): add VlcPlayer wrapper for libvlc"
```

---

### Task 3: Swap TvPlayer for VlcPlayer in LiveTvScreen

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt`

- [ ] **Step 1: Replace imports**

```kotlin
// Remove these two lines:
// import com.rukatv.iptv.player.TvPlayer
// import com.rukatv.iptv.player.StreamKind

// Add this line:
import com.rukatv.iptv.player.VlcPlayer
```

- [ ] **Step 2: Remove streamKindOf function + comment (lines 70-79)**

Delete:

```kotlin
// Deduce el tipo de stream a partir de la URL original que guarda el backend.
// Las URLs tipo /play/xxxx (sin extensión de contenedor) o .ts son MPEG-TS en
// vivo; las .m3u8/.m3u son HLS. El backend hace proxy de ambos por la misma
// ruta /live/.../{id}.m3u8, así que la app debe forzar el extractor correcto.
fun streamKindOf(streamUrl: String): StreamKind {
    val lower = streamUrl.lowercase()
    if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) return StreamKind.HLS
    if (lower.endsWith(".ts") || lower.contains("/play/")) return StreamKind.TS
    return StreamKind.PROGRESSIVE
}
```

- [ ] **Step 3: Replace player instantiation and view**

```kotlin
// Before (line 111):
val player = remember { TvPlayer(context) }

// After:
val player = remember { VlcPlayer(context) }
```

```kotlin
// Before (line 112-117):
LaunchedEffect(Unit) {
    if (filtered.isNotEmpty()) {
        val ch = filtered[0]
        player.prepare(catalog.liveUrl(ch.streamId), streamKindOf(ch.streamUrl))
    }
}
DisposableEffect(Unit) { onDispose { player.release() } }

val playerView = remember { player.playerView(context) }

// After:
LaunchedEffect(Unit) {
    if (filtered.isNotEmpty()) {
        player.play(filtered[0].streamUrl)
    }
}
DisposableEffect(Unit) { onDispose { player.stop(); player.release() } }
```

- [ ] **Step 4: Replace playIndex function (lines 125-131)**

```kotlin
// Before:
fun playIndex(i: Int) {
    selectedIndex = i
    if (filtered.isNotEmpty()) {
        val ch = filtered[i]
        player.prepare(catalog.liveUrl(ch.streamId), streamKindOf(ch.streamUrl))
    }
}

// After:
fun playIndex(i: Int) {
    selectedIndex = i
    if (filtered.isNotEmpty()) {
        player.stop()
        player.play(filtered[i].streamUrl)
    }
}
```

- [ ] **Step 5: Update AndroidView in fullscreen mode (lines 181-195)**

Replace `playerView` with `player.view`:

```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
        (player.view.parent as? ViewGroup)?.removeView(player.view)
        android.widget.FrameLayout(ctx).also { fl ->
            fl.addView(
                player.view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }
)
```

- [ ] **Step 6: Update AndroidView in split view mode (lines 309-323)**

Same change — replace `playerView` with `player.view`:

```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
        (player.view.parent as? ViewGroup)?.removeView(player.view)
        android.widget.FrameLayout(ctx).also { fl ->
            fl.addView(
                player.view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }
)
```

- [ ] **Step 7: Build and verify**

Run: `cd android && ./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt
git commit -m "feat(live-tv): replace ExoPlayer with VlcPlayer for native libvlc playback"
```
