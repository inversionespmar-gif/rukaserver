# Resume Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remember where the user left off in movies/series and show a "Continue watching?" prompt on return.

**Architecture:** `PlaybackProgressStore` wraps DataStore Preferences to persist `url → positionMs` pairs. `PlayerScreen` saves progress every 5s and on pause/exit, and shows a Netflix-style resume overlay on load. `HomeScreen` instantiates the store and passes it down.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, AndroidX Media3 (ExoPlayer)

---

### Task 1: Create PlaybackProgressStore

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/data/local/PlaybackProgressStore.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.rukatv.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.progressDataStore by preferencesDataStore(name = "playback_progress")

class PlaybackProgressStore(private val context: Context) {
    private val progressKey = stringPreferencesKey("progress")

    fun getProgress(url: String): Flow<Long?> =
        context.progressDataStore.data.map { prefs ->
            val json = prefs[progressKey]
            if (json.isNullOrBlank()) return@map null
            val obj = JSONObject(json)
            if (obj.has(url)) obj.getLong(url) else null
        }

    suspend fun saveProgress(url: String, positionMs: Long) {
        context.progressDataStore.edit { prefs ->
            val json = prefs[progressKey]
            val obj = if (json.isNullOrBlank()) JSONObject() else JSONObject(json)
            obj.put(url, positionMs)
            prefs[progressKey] = obj.toString()
        }
    }

    suspend fun removeProgress(url: String) {
        context.progressDataStore.edit { prefs ->
            val json = prefs[progressKey]
            if (json.isNullOrBlank()) return@edit
            val obj = JSONObject(json)
            obj.remove(url)
            prefs[progressKey] = obj.toString()
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/data/local/PlaybackProgressStore.kt
git commit -m "feat(data): add PlaybackProgressStore for resume position persistence"
```

---

### Task 2: Add progress saving + resume overlay to PlayerScreen

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt`

- [ ] **Step 1: Add new imports**

At the top, after the existing imports:

```kotlin
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rukatv.iptv.data.local.PlaybackProgressStore
import kotlinx.coroutines.flow.first
```

- [ ] **Step 2: Add `progressStore` parameter and internal state**

Change the function signature from:
```kotlin
fun PlayerScreen(
    queue: List<PlayItem>,
    startIndex: Int = 0,
    isSeries: Boolean = false,
    onExit: () -> Unit
)
```
to:
```kotlin
fun PlayerScreen(
    queue: List<PlayItem>,
    startIndex: Int = 0,
    isSeries: Boolean = false,
    progressStore: PlaybackProgressStore,
    onExit: () -> Unit
)
```

Add these state variables after `var controlsVisible by remember { mutableStateOf(true) }`:

```kotlin
    var resumeOverlayVisible by remember { mutableStateOf(false) }
    var resumePosition by remember { mutableStateOf(0L) }
    var resumeTimestampText by remember { mutableStateOf("") }
```

- [ ] **Step 3: Add progress check when index changes**

In the existing `LaunchedEffect(index)` (the one that prepares the player), add at the **beginning** before the existing code:

```kotlin
    LaunchedEffect(index) {
        showNextPrompt = false
        countdown = 10

        // Check for saved progress
        val playUrl = queue.getOrNull(index)?.url
        if (playUrl != null) {
            val savedPos = progressStore.getProgress(playUrl).first()
            if (savedPos != null && savedPos > 0) {
                resumePosition = savedPos
                val totalSec = savedPos / 1000
                val m = totalSec / 60
                val s = totalSec % 60
                resumeTimestampText = "%d:%02d".format(m, s)
                resumeOverlayVisible = true
            }
        }

        if (queue.isNotEmpty()) {
            player.prepare(queue[index.coerceIn(0, queue.lastIndex)].url)
        }
        // ... rest unchanged
```

- [ ] **Step 4: Add periodic progress save LaunchedEffect**

Add after the countdown `LaunchedEffect(showNextPrompt)` block:

```kotlin
    // Save progress every 5 seconds while playing
    LaunchedEffect(index) {
        while (true) {
            delay(5000)
            val url = queue.getOrNull(index)?.url ?: continue
            if (player.player.isPlaying) {
                progressStore.saveProgress(url, player.player.currentPosition)
            }
        }
    }
```

- [ ] **Step 5: Add save on pause to the Player.Listener**

Modify the existing `player.setListener(...)` block. Replace the current listener (lines starting with `LaunchedEffect(Unit) { player.setListener(...)`) with this version that adds `onPlayWhenReadyChanged`:

```kotlin
    // Detect end of media -> autoplay next (Netflix style) for series.
    LaunchedEffect(Unit) {
        player.setListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val q = currentQueue
                    val idx = currentIndex
                    if (idx <= q.lastIndex) {
                        // Mark as completed
                        val ctx = context
                        kotlinx.coroutines.runBlocking {
                            PlaybackProgressStore(ctx).removeProgress(q[idx].url)
                        }
                    }
                    if (currentIsSeries && idx < q.lastIndex) {
                        showNextPrompt = true
                    } else {
                        onExit()
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    val url = currentQueue.getOrNull(currentIndex)?.url ?: return
                    val scope = rememberCoroutineScope()
                    scope.launch {
                        progressStore.saveProgress(url, player.player.currentPosition)
                    }
                }
            }
        })
    }
```

Wait, `rememberCoroutineScope()` can't be called inside a listener. Let me rethink this.

The problem is that `onPlayWhenReadyChanged` is called from the player's thread. I need a coroutine scope. Options:
1. Use `GlobalScope.launch` (bad practice)
2. Capture the coroutine scope from the composable

Actually, looking at the existing code, the `scope` variable is already declared at line ~106 (`val scope = rememberCoroutineScope()`). So I can reference that. Let me adjust the code.

Actually, the existing listener code is inside a `LaunchedEffect(Unit)` which provides a coroutine scope via the suspend function. But the listener methods like `onPlaybackStateChanged` and `onPlayWhenReadyChanged` are callbacks, not suspend functions. I need a coroutine scope to call `progressStore.saveProgress()` which is a suspend function.

I can capture the scope from the composable. Let me use the existing `scope` which is already declared as `val scope = rememberCoroutineScope()`.

Also, in the `onPlaybackStateChanged` where I added `runBlocking`, that's really bad practice. Let me use `scope.launch` instead.

Let me rewrite the listener block properly:

```kotlin
    // Detect end of media -> autoplay next (Netflix style) for series.
    LaunchedEffect(Unit) {
        player.setListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val q = currentQueue
                    val idx = currentIndex
                    if (idx <= q.lastIndex) {
                        scope.launch {
                            progressStore.removeProgress(q[idx].url)
                        }
                    }
                    if (currentIsSeries && idx < q.lastIndex) {
                        showNextPrompt = true
                    } else {
                        onExit()
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    val url = currentQueue.getOrNull(currentIndex)?.url ?: return
                    scope.launch {
                        progressStore.saveProgress(url, player.player.currentPosition)
                    }
                }
            }
        })
    }
```

This requires `scope` to be accessible (it's already declared in the composable function). And `progressStore` to be accessible (it's now a parameter). Good.

But I need to make sure the `scope` is accessible inside the listener. In the current code, `scope` is declared at `val scope = rememberCoroutineScope()` after the `favSet` line. Since the listener is defined inside a `LaunchedEffect(Unit)`, it has access to the composable's scope. Yes, this should work because `scope` is a `rememberCoroutineScope` which is available throughout the composable.

Actually wait, there's a subtle issue. The `player.setListener()` call is inside a `LaunchedEffect(Unit)`. The `scope` variable from `rememberCoroutineScope()` should be accessible from inside the LaunchedEffect because the lambda captures the composable's scope. Yes, this works.

But wait, the current code has:

```kotlin
    val scope = rememberCoroutineScope()
    // ...
    LaunchedEffect(Unit) {
        player.setListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                // ...
            }
        })
    }
```

`LaunchedEffect` runs in a coroutine scope, and the listener callbacks are invoked from the player thread. Using `scope.launch` (the `rememberCoroutineScope`) inside those callbacks should work fine.

Actually, hold on - I just realized that `scope` might not be in the correct lexical scope. Let me check the actual code structure...

Looking at the code, `scope` is declared at the composable level, and `LaunchedEffect(Unit)` is also at the composable level. The `LaunchedEffect` block has access to the composable's scope (including `scope`). The listener inside it also captures `scope` from the closure. This should work.

OK, let me also handle the on-exit progress save. I'll add it to the `DisposableEffect(Unit)`:

```kotlin
    DisposableEffect(Unit) {
        val prevOrientation = activity?.requestedOrientation
            ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            // Save progress on exit
            val scope2 = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
            scope2.launch {
                val url = queue.getOrNull(index)?.url ?: return@launch
                progressStore.saveProgress(url, player.player.currentPosition)
            }
            player.release()
            activity?.requestedOrientation = prevOrientation
        }
    }
```

Actually, creating a new CoroutineScope in onDispose is fine but using `runBlocking` would be simpler and more reliable here since we need to save before releasing the player:

Hmm, `runBlocking` is synchronous and would block the dispose thread. That's not ideal but it's in the dispose path. Actually, let me just use `runBlocking` for simplicity:

Wait, actually the simplest approach: save progress before releasing in the disposer. Since `DisposableEffect.onDispose` runs on the main thread, I can use a simple synchronous save. But `saveProgress` is a suspend function...

Let me use `runBlocking`:

```kotlin
        onDispose {
            runBlocking {
                val url = queue.getOrNull(index)?.url ?: return@runBlocking
                progressStore.saveProgress(url, player.player.currentPosition)
            }
            player.release()
            activity?.requestedOrientation = prevOrientation
        }
```

This is fine - `runBlocking` on dispose is a common pattern.

Now for the resume overlay UI. I'll add it as an `AnimatedVisibility` block inside the Box, after the skip intro overlay:

```kotlin
        // Resume playback overlay
        AnimatedVisibility(
            visible = resumeOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6121212))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Continuar viendo?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        resumeTimestampText,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Accent)
                                .clickable {
                                    player.player.seekTo(resumePosition)
                                    resumeOverlayVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Continuar",
                                color = Color(0xFF06231F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                                .clickable {
                                    scope.launch {
                                        val url = queue.getOrNull(index)?.url ?: return@launch
                                        progressStore.removeProgress(url)
                                    }
                                    resumeOverlayVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Empezar de nuevo",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
```

I also need to add auto-dismiss after 5 seconds:

```kotlin
    LaunchedEffect(resumeOverlayVisible) {
        if (resumeOverlayVisible) {
            delay(5000)
            resumeOverlayVisible = false
        }
    }
```

Let me also need to add `import androidx.compose.foundation.layout.Spacer` and `import androidx.compose.foundation.layout.height`.

OK, the plan is getting quite detailed. Let me write it step by step.<｜end▁of▁thinking｜>

<｜｜DSML｜｜tool_calls>
<｜｜DSML｜｜invoke name="write">
<｜｜DSML｜｜parameter name="content" string="true"># Resume Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remember where the user left off in movies/series and show a "Continue watching?" prompt on return.

**Architecture:** `PlaybackProgressStore` wraps DataStore Preferences to persist `url → positionMs` pairs. `PlayerScreen` saves progress every 5s and on pause/exit, and shows a Netflix-style resume overlay on load. `HomeScreen` instantiates the store and passes it down.

**Tech Stack:** Kotlin, Jetpack Compose, DataStore Preferences, AndroidX Media3 (ExoPlayer)

---

### Task 1: Create PlaybackProgressStore

**Files:**
- Create: `android/app/src/main/java/com/rukatv/iptv/data/local/PlaybackProgressStore.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.rukatv.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.progressDataStore by preferencesDataStore(name = "playback_progress")

class PlaybackProgressStore(private val context: Context) {
    private val progressKey = stringPreferencesKey("progress")

    fun getProgress(url: String): Flow<Long?> =
        context.progressDataStore.data.map { prefs ->
            val json = prefs[progressKey]
            if (json.isNullOrBlank()) return@map null
            val obj = JSONObject(json)
            if (obj.has(url)) obj.getLong(url) else null
        }

    suspend fun saveProgress(url: String, positionMs: Long) {
        context.progressDataStore.edit { prefs ->
            val json = prefs[progressKey]
            val obj = if (json.isNullOrBlank()) JSONObject() else JSONObject(json)
            obj.put(url, positionMs)
            prefs[progressKey] = obj.toString()
        }
    }

    suspend fun removeProgress(url: String) {
        context.progressDataStore.edit { prefs ->
            val json = prefs[progressKey]
            if (json.isNullOrBlank()) return@edit
            val obj = JSONObject(json)
            obj.remove(url)
            prefs[progressKey] = obj.toString()
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/data/local/PlaybackProgressStore.kt
git commit -m "feat(data): add PlaybackProgressStore for resume position persistence"
```

---

### Task 2: Wire PlaybackProgressStore in HomeScreen

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/HomeScreen.kt`

- [ ] **Step 1: Add import and create store**

Add at top:
```kotlin
import com.rukatv.iptv.data.local.PlaybackProgressStore
import androidx.compose.ui.platform.LocalContext
```

Inside `HomeScreen`, after `var fullscreen by remember { mutableStateOf(false) }`:
```kotlin
    val ctx = LocalContext.current
    val progressStore = remember { PlaybackProgressStore(ctx) }
```

- [ ] **Step 2: Pass store to PlayerScreen**

Find where `onPlayQueue` is called (or where PlayerScreen is opened) and ensure `progressStore` is passed. Look for the navigation that leads to PlayerScreen (likely in the `when (current)` block or similar). If PlayerScreen is created inside HomeScreen, add `progressStore = progressStore` to its arguments.

- [ ] **Step 3: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/HomeScreen.kt
git commit -m "feat(home): wire PlaybackProgressStore to PlayerScreen"
```

---

### Task 3: Add progress saving + resume overlay to PlayerScreen

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt`

- [ ] **Step 1: Add imports**

At the top:
```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.rukatv.iptv.data.local.PlaybackProgressStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
```

- [ ] **Step 2: Add `progressStore` parameter**

Change signature from:
```kotlin
    onExit: () -> Unit
```
to:
```kotlin
    progressStore: PlaybackProgressStore,
    onExit: () -> Unit
```

- [ ] **Step 3: Add resume overlay state vars**

After `var controlsVisible by remember { mutableStateOf(true) }`:
```kotlin
    var resumeOverlayVisible by remember { mutableStateOf(false) }
    var resumePosition by remember { mutableStateOf(0L) }
    var resumeTimestampText by remember { mutableStateOf("") }
```

- [ ] **Step 4: Check saved progress when index changes**

Modify the existing `LaunchedEffect(index)` that prepares the player. Add at the beginning:
```kotlin
    LaunchedEffect(index) {
        showNextPrompt = false
        countdown = 10

        // Check for saved progress
        val playUrl = queue.getOrNull(index)?.url
        if (playUrl != null) {
            val savedPos = progressStore.getProgress(playUrl).first()
            if (savedPos != null && savedPos > 0) {
                resumePosition = savedPos
                val totalSec = savedPos / 1000
                resumeTimestampText = "%d:%02d".format(totalSec / 60, totalSec % 60)
                resumeOverlayVisible = true
            }
        }

        if (queue.isNotEmpty()) {
            player.prepare(queue[index.coerceIn(0, queue.lastIndex)].url)
        }
        // ... rest (skip intro) unchanged
```

- [ ] **Step 5: Add periodic progress save**

After the countdown `LaunchedEffect(showNextPrompt)`:
```kotlin
    // Save progress every 5 seconds while playing
    LaunchedEffect(index) {
        while (true) {
            delay(5000)
            val url = queue.getOrNull(index)?.url ?: continue
            if (player.player.isPlaying) {
                progressStore.saveProgress(url, player.player.currentPosition)
            }
        }
    }
```

- [ ] **Step 6: Add save on pause + remove on end in Player.Listener**

Replace the existing `player.setListener(...)` block with:
```kotlin
    LaunchedEffect(Unit) {
        player.setListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    val q = currentQueue
                    val idx = currentIndex
                    if (idx <= q.lastIndex) {
                        scope.launch { progressStore.removeProgress(q[idx].url) }
                    }
                    if (currentIsSeries && idx < q.lastIndex) {
                        showNextPrompt = true
                    } else {
                        onExit()
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    val url = currentQueue.getOrNull(currentIndex)?.url ?: return
                    scope.launch {
                        progressStore.saveProgress(url, player.player.currentPosition)
                    }
                }
            }
        })
    }
```

- [ ] **Step 7: Save progress on exit**

In `DisposableEffect(Unit).onDispose`, add before `player.release()`:
```kotlin
            runBlocking {
                val url = queue.getOrNull(index)?.url ?: return@runBlocking
                progressStore.saveProgress(url, player.player.currentPosition)
            }
```

- [ ] **Step 8: Auto-dismiss overlay after 5s**

Add after the `LaunchedEffect(index)` blocks:
```kotlin
    LaunchedEffect(resumeOverlayVisible) {
        if (resumeOverlayVisible) {
            delay(5000)
            resumeOverlayVisible = false
        }
    }
```

- [ ] **Step 9: Add resume overlay UI**

Inside the `Box(Modifier.fillMaxSize().background(Color.Black))`, right before the closing `}` of the skip intro overlay or after it, add:
```kotlin
        // Resume playback overlay
        AnimatedVisibility(
            visible = resumeOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6121212))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Continuar viendo?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        resumeTimestampText,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Accent)
                                .clickable {
                                    player.player.seekTo(resumePosition)
                                    resumeOverlayVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Continuar",
                                color = Color(0xFF06231F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                                .clickable {
                                    scope.launch {
                                        val url = queue.getOrNull(index)?.url ?: return@launch
                                        progressStore.removeProgress(url)
                                    }
                                    resumeOverlayVisible = false
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Empezar de nuevo",
                                color = Color(0xFFCCCCCC),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
```

- [ ] **Step 10: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "feat(player): add resume playback with progress saving and overlay"
```

---

### Task 4: Build and manual verification

**Files:** (none — manual test)

- [ ] **Step 1: Build**

Run: `cd android; ./gradlew installDebug`

- [ ] **Step 2: Verify on device**
- [ ] Reproduce un video, pausa a los ~30s, sal.
- [ ] Vuelve a abrir el mismo video → aparece overlay "Continuar viendo?" con timestamp.
- [ ] "Continuar" reanuda desde la posición guardada.
- [ ] "Empezar de nuevo" borra el progreso y empieza desde 0.
- [ ] Deja el overlay 5s sin tocar → se cierra solo.
- [ ] Reproduce un video completo → al volver no aparece overlay.
- [ ] Serie: cada episodio tiene su propio progreso.

- [ ] **Step 3: Commit any tweaks**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "fix(player): adjust resume behavior after device test"
```
