# Live TV Remote Navigation & Compact Channel List — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the fullscreen Live TV channel overlay navigable with the TV remote (OK opens, Up/Down move, OK tunes & closes) and render the channel list compact and right-anchored (~400dp) instead of full-width.

**Architecture:** Restructure the fullscreen overlay in `LiveTvScreen.kt` so the background scrim closes on outside click only, while a right-anchored, focusable `LazyColumn` receives remote focus (`FocusRequester` + `LazyListState`). Keep `ChannelRow` filling its (now narrow) parent so the grey line shrinks automatically. Reuse existing `playIndex`, `onFocus`, `onClick` and `BackHandler` logic.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), `androidx.compose.foundation.lazy.LazyColumn`, `FocusRequester`, `androidx.compose.ui.input.key.onKeyEvent`.

---

### Task 1: Open channel overlay with OK/Enter key in fullscreen

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt:143-148` (the root `Box` that already has `onKeyEvent` for digits)
- Modify: same file fullscreen overlay open-clicks at lines `168` and `175`

- [ ] **Step 1: Add imports for remote keys**

At the top of `LiveTvScreen.kt`, ensure these imports exist (add after the existing `onKeyEvent` imports):

```kotlin
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyEvent
```

- [ ] **Step 2: Extend the digit-capture `onKeyEvent` to also toggle overlay on Enter/OK**

Replace the root `Box` key handler (lines 143-148):

```kotlin
    Box(Modifier.fillMaxSize().onKeyEvent { ev ->
        if (ev.type == KeyEventType.KeyDown) {
            val c = ev.nativeKeyEvent.unicodeChar.toChar()
            if (c.isDigit()) { numberBuffer += c; true }
            else if (ev.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER
                || ev.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                if (fullscreen) { overlay = !overlay; true } else false
            }
            else false
        } else false
    }) {
```

- [ ] **Step 3: Verify the file still compiles**

Run: `cd android; ./gradlew compileDebugKotlin` (or `gradlew.bat compileDebugKotlin` on Windows)
Expected: BUILD SUCCESSFUL (no new errors). Note: this only adds open-by-remote; closing is still via the scrim click until Task 2.

- [ ] **Step 4: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt
git commit -m "feat(livetv): open channel overlay with OK/Enter in fullscreen"
```

---

### Task 2: Restructure overlay layout — right-anchored, focus-friendly scrim

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt:169-190` (the `if (overlay) { ... }` block)

- [ ] **Step 1: Add imports for FocusRequester, alignment, lazy list state**

Ensure these imports exist near the others:

```kotlin
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
```

- [ ] **Step 2: Declare a dedicated FocusRequester and LazyListState for the overlay**

Inside `LiveTvScreen`, right after the existing `val listState = rememberLazyListState()` (line 115), add:

```kotlin
    val overlayListState = rememberLazyListState()
    val overlayFocusRequester = remember { FocusRequester() }
```

- [ ] **Step 3: Replace the overlay `if (overlay)` block**

Replace lines 169-190 (`if (overlay) { Box(Modifier.fillMaxSize()...` through its closing `}`) with:

```kotlin
                // Channel list overlay
                if (overlay) {
                    // Scrim: clicking outside the list closes it. Does NOT steal focus.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xCC000000))
                            .clickable { overlay = false }
                    )
                    // Right-anchored, compact, focusable list.
                    LazyColumn(
                        state = overlayListState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(400.dp)
                            .padding(16.dp)
                            .focusRequester(overlayFocusRequester)
                            .focusable(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filtered) { i, ch ->
                            ChannelRow(
                                index = i,
                                name = ch.name,
                                logo = ch.streamIcon,
                                onFocus = { playIndex(i) },
                                onClick = { playIndex(i); overlay = false }
                            )
                        }
                    }
                }
```

- [ ] **Step 4: Verify it compiles**

Run: `cd android; ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt
git commit -m "feat(livetv): right-anchored, focusable compact channel overlay"
```

---

### Task 3: Move focus into the list and start selection on the tuned channel

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt` (add a `LaunchedEffect` near the other effects, e.g. after line 141 `BackHandler`)

- [ ] **Step 1: Add a LaunchedEffect that requests focus and scrolls to the current channel when overlay opens**

Insert after the `BackHandler` block (lines 139-141):

```kotlin
    LaunchedEffect(overlay) {
        if (overlay) {
            overlayListState.scrollToItem(selectedIndex.coerceAtLeast(0))
            overlayFocusRequester.requestFocus()
        }
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `cd android; ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt
git commit -m "feat(livetv): focus channel list and start on tuned channel on open"
```

---

### Task 4: Constrain ChannelRow width so the grey line is compact

**Files:**
- Modify: `android/app/src/main/java/com/rukatv/iptv/ui/components/ChannelRow.kt:44-58` (the `Row` modifier block)

- [ ] **Step 1: Replace `fillMaxWidth()` with width constrained to the parent**

In `ChannelRow`, the `Row` modifier currently starts with `.fillMaxWidth()`. Replace that line so the row fills its (now 400.dp) parent but never exceeds it:

```kotlin
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .clip(RoundedCornerShape(10.dp))
```

Note: `widthIn` keeps it filling the parent when the parent is the 400.dp overlay (so the grey line spans the compact list, not the full screen). The `modifier` param is already applied first, so callers that pass no modifier are unaffected.

- [ ] **Step 2: Verify it compiles**

Run: `cd android; ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/components/ChannelRow.kt
git commit -m "feat(channelrow): constrain width so grey line is compact"
```

---

### Task 5: End-to-end manual verification on Android TV / emulator

**Files:** (none — manual test)

- [ ] **Step 1: Build and install debug APK**

Run: `cd android; ./gradlew installDebug` (target a TV emulator or connected Android TV device with `adb`).

- [ ] **Step 2: Verify acceptance criteria**

Manually confirm on device:
- [ ] In fullscreen, pressing OK/Enter OR clicking opens the channel list (right side, ~400dp, compact grey lines).
- [ ] With the list open, Up/Down move the highlighted (focused) channel; focused channel previews (audio/change) via `onFocus = playIndex`.
- [ ] Pressing OK on a channel tunes it and closes the list.
- [ ] Back closes the list and returns focus to the video (existing `BackHandler`).
- [ ] The grey line of each channel is compact (no longer full-screen width).
- [ ] Focused state is clearly visible (Accent border + background + ▶ arrow, already in `ChannelRow`).

- [ ] **Step 3: Final commit of any tweaks**

If minor visual/behavior tweaks are needed, commit them:

```bash
cd "C:\Users\Coopchinokue Ltda\Documents\GitHub\Rukaserver"
git add android/app/src/main/java/com/rukatv/iptv/ui/
git commit -m "fix(livetv): adjust overlay focus/width after device test"
```
