# PlayerScreen Remote Control (Netflix-style) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full D-pad and media key support in PlayerScreen with smart context-aware behavior (controls hidden vs visible).

**Architecture:** A single `View.OnKeyListener` attached to the `PlayerView` intercepts keys before the internal handler. A `rememberUpdatedState` ref lets the listener read the latest `controlsVisible` Compose state. The listener handles DPAD_CENTER (play/pause when hidden, passthrough when visible), DPAD_LEFT/RIGHT (seek when hidden, passthrough when visible), MEDIA_PLAY/PAUSE/PLAY_PAUSE (toggle), and MEDIA_NEXT/PREVIOUS (episode skip). All key presses also show the controller overlay and reset the auto-hide timer.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3 (ExoPlayer/PlayerView), Android TV

---

### Task 1: Add reactive state ref and episode navigation helpers

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt:71-84` (after `controlsVisible` declaration, before the `DisposableEffect`)

- [ ] **Step 1: Add import for updated state ref**

Add at top (near existing `rememberUpdatedState` import — it's already there):

```kotlin
// rememberUpdatedState is already imported at line 31
```

- [ ] **Step 2: Add `nextEpisode()` and `prevEpisode()` after `controlsVisible`**

After line 71 (`var controlsVisible by remember { mutableStateOf(true) }`):

```kotlin
    fun nextEpisode() {
        if (index < queue.lastIndex) {
            player.player.stop()
            index += 1
            showNextPrompt = false
        }
    }

    fun prevEpisode() {
        if (index > 0) {
            player.player.stop()
            index -= 1
            showNextPrompt = false
        }
    }
```

Note: `player.player` is the public ExoPlayer instance inside `TvPlayer`.

- [ ] **Step 3: Verify syntax**

Read the file to check for any obvious syntax errors. The file should compile after this change.

- [ ] **Step 4: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "feat(player): add reactive controlsVisibleRef and episode nav helpers"
```

---

### Task 2: Attach OnKeyListener to PlayerView with full key mapping

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt:131-148` (the `AndroidView` / `PlayerView` factory block)

- [ ] **Step 1: Add import for `android.view.KeyEvent`**

At the top:

```kotlin
import android.view.KeyEvent
```

- [ ] **Step 2: Add `controlsVisibleRef` before the Box and replace PlayerView creation**

Add `val controlsVisibleRef = rememberUpdatedState(controlsVisible)` on the line BEFORE the `Box(Modifier.fillMaxSize()...)`. Then replace the `AndroidView { factory -> ... }` block inside it with the version that includes the key listener:

```kotlin
    val controlsVisibleRef = rememberUpdatedState(controlsVisible)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Full-screen video
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { fl ->
                    val pv = player.playerView(ctx) { visible ->
                        controlsVisible = visible
                    }
                    pv.setOnKeyListener { _, keyCode, event ->
                        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                        val ctrlVisible = controlsVisibleRef.value
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER -> {
                                if (!ctrlVisible) {
                                    player.player.playWhenReady = !player.player.playWhenReady
                                    pv.showController()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (!ctrlVisible) {
                                    val newPos = (player.player.currentPosition - 10000).coerceAtLeast(0L)
                                    player.player.seekTo(newPos)
                                    pv.showController()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (!ctrlVisible) {
                                    val newPos = player.player.currentPosition + 10000
                                    player.player.seekTo(newPos)
                                    pv.showController()
                                    true
                                } else false
                            }
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                player.player.playWhenReady = !player.player.playWhenReady
                                pv.showController()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                nextEpisode()
                                pv.showController()
                                true
                            }
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                                prevEpisode()
                                pv.showController()
                                true
                            }
                            else -> {
                                pv.showController()
                                false
                            }
                        }
                    }
                    fl.addView(
                        pv,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }
        )
        // ... rest of the AnimatedVisibility sections unchanged
```

Note: Keep all the `AnimatedVisibility` blocks and everything below exactly as-is. Only the `AndroidView` block above changes.

- [ ] **Step 3: Handle nextEpisode/prevEpisode potentially referring to `player.player.stop()`**

Since `player.player.stop()` is called before changing index, the `LaunchedEffect(index)` will then call `player.prepare(...)`. Verify that `player.player` is public in `TvPlayer.kt`. It is — `val player = ExoPlayer.Builder(...)...build()` (line 33).

- [ ] **Step 4: Compile verification**

Run: `cd android; ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If errors, address them (likely import issues).

- [ ] **Step 6: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "feat(player): add OnKeyListener for Netflix-style remote control"
```

---

### Task 3: End-to-end verification on device

**Files:** (none — manual test)

- [ ] **Step 1: Build and install**

Run: `cd android; ./gradlew installDebug` on a TV device or emulator.

- [ ] **Step 2: Verify acceptance criteria**

- [ ] OK/Enter with overlay hidden: toggles play/pause.
- [ ] OK/Enter with overlay visible: navigates UI buttons (default PlayerView behavior).
- [ ] DPAD Left with overlay hidden: seek backward 10s.
- [ ] DPAD Right with overlay hidden: seek forward 10s.
- [ ] DPAD Left/Right with overlay visible: navigate UI (default PlayerView behavior).
- [ ] MEDIA_NEXT: advances to next episode in queue.
- [ ] MEDIA_PREVIOUS: goes to previous episode in queue.
- [ ] Any key press: shows overlay controls + resets auto-hide timer.

- [ ] **Step 3: Commit any final tweaks**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "fix(player): adjust remote control behavior after device test"
```
