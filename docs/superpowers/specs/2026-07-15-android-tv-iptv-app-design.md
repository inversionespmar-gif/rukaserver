# Design: Android TV IPTV App (estilo IPTV Smarters)

- **Date:** 2026-07-15
- **Status:** Approved (design)
- **Author:** opencode (brainstorming session)
- **Target server:** Rukaserver Xtream Codes API (e.g. `https://rukaserver-1.onrender.com`)

## 1. Overview

A native Android TV application (also runs on Android phones/tablets) that connects to an
Xtream Codes-compatible server and plays Live TV, Movies, and Series. The UI follows the
**IPTV Smarters** look: a vertical navigation rail on the left and content on the right, with a
cyan (`#2ED3C6`) accent on a dark background.

The headline feature is the **Live TV screen**: an enumerated channel list on the left with an
auto-playing player on the right, remote-control navigation, jump-by-number/name, and a
fullscreen mode that overlays the channel list while video keeps playing.

> Note: The dev environment has no Android SDK, so the implementation is delivered as a complete
> Gradle/Android project. The user builds and runs it in Android Studio. No build step runs here.

## 2. Goals

- Native Android TV app (Leanback launcher) in Kotlin.
- Play Live / Movies / Series from the Rukaserver Xtream API.
- IPTV Smarters-style UI with cyan accent.
- Live TV: enumerated list + auto-play side player + fullscreen + channel-overlay switching.
- Search (by name) and Favorites (starred items).
- Login screen (host / user / password) with credentials stored locally (DataStore), not hardcoded.

## 3. Non-goals (YAGNI)

- EPG grid with multi-day schedule.
- Multi-screen / picture-in-picture mosaic.
- Catch-up / recording / timeshift.
- Multiple user profiles within one login.
- External subtitle/audio track selection UI (ExoPlayer defaults only).

## 4. Tech Stack

| Concern        | Choice                                              |
|----------------|-----------------------------------------------------|
| Language       | Kotlin                                             |
| UI             | Jetpack Compose for TV (`androidx.tv-material`)    |
| Player         | Media3 ExoPlayer (`androidx.media3`) — HLS native  |
| Networking     | Retrofit2 + Moshi                                  |
| Concurrency    | Kotlin Coroutines + StateFlow                      |
| Local storage  | DataStore Preferences (credentials)                |
| Navigation     | Compose navigation / screen state (TV-friendly)    |
| Build          | Gradle Kotlin DSL, Android Gradle Plugin           |

minSdk = 21 (Android TV minimum), targetSdk = latest stable. TV manifest: `LEANBACK_LAUNCHER`
intent, `android:isGame="false"`, banner drawable.

## 5. Architecture

```
ui/                 Composable screens + theme
  theme/            ColorScheme (cyan accent), Typography, Spacing
  screens/          Login, Home, LiveTv, Movies, Series, Search, Favorites, Player
  components/       ChannelRow, PosterCard, NavRail, Chip, LoadingState
data/
  remote/           XtreamApi (Retrofit), DTOs, UrlBuilder
  repository/       CatalogRepository, AuthRepository, FavoritesRepository
  local/            CredentialsStore (DataStore), FavoritesStore (DataStore)
player/
  TvPlayer.kt       ExoPlayer wrapper: prepare(url), play/pause, release
  PlayerState.kt    UI state for controls/errors
```

Unidirectional flow: Screen collects `StateFlow` from a ViewModel; ViewModel calls repository;
repository calls `XtreamApi` or local stores.

## 6. Server API & URL contract

Base URL entered by user at login (e.g. `https://rukaserver-1.onrender.com`). Credentials are
embedded in the **path** of stream URLs (no extra auth header needed).

- Auth / catalog: `GET {base}/player_api.php?username={u}&password={p}&action={action}`
  - actions: `get_live_categories`, `get_live_streams` (opt `category_id`),
    `get_vod_categories`, `get_vod_streams`, `get_series_categories`, `get_series`,
    `get_series_info` (param `series_id`).
- **Stream URLs built by the app from IDs** (do NOT use the raw `stream_url` field):
  - Live:   `{base}/live/{u}/{p}/{stream_id}.m3u8`
  - Movie:  `{base}/movie/{u}/{p}/{stream_id}.mp4`  (server resolves → returns m3u8)
  - Series: `{base}/series/{u}/{p}/{episode_stream_id}.m3u8`

### 6.1 Key response shapes (from `src/repositories/catalog.js`)

- Live stream: `{ stream_id, name, stream_icon, category_id (pais), stream_url }`
- Live category: `{ category_id, category_name }` (category_id = country)
- VOD: `{ stream_id, name, poster (TMDB w500 url), plot, release_date, rating }`
- Series: `{ series_id, name, cover/poster, plot, release_date, rating }`
- Series info: `{ seasons:[{season_number,name,cover}], info:{name,plot,poster_path,backdrop_path,rating}, episodes:{ "<n>":[{id,episode_num,title,season,stream_id}] } }`

## 7. Screens

### 7.1 Login
- Fields: Host, Username, Password. No pre-filled values.
- On submit: call `player_api.php` (no action) → expect `user_info.auth == 1`.
- Success → save credentials to DataStore → navigate Home.
- Error → inline message; never store invalid creds.

### 7.2 Home / Browse
- Left `NavRail`: TV en vivo · Películas · Series · Buscar · Favoritos (icons + labels).
- Right content area shows the selected section.

### 7.3 Live TV (headline feature)
Layout: two panes.
- **Left pane — enumerated channel list**: `LazyColumn` of channels, each row shows index
  number `1..N`, logo (`stream_icon`) and name. Rows are focusable (DPAD up/down).
  - **Jump by number**: digit keys on the remote set a pending number; after a short debounce
    the list scrolls to / selects that channel index.
  - **Search by name**: a text field (filtered) at top of the list (remote text entry acceptable).
  - Channels grouped/filterable by category (country) via chips above the list.
- **Right pane — auto-playing player**: a `PlayerView` (ExoPlayer) that **automatically plays**
  the currently focused/selected channel. Moving focus in the list updates the selected channel
  and the player prepares/swaps to that source.
- **Fullscreen**: when focus is on the player and the user clicks (DPAD center), open the
  immersive `PlayerScreen` (fullscreen, hide nav rail).
- **Channel overlay**: inside fullscreen, a click toggles a **channel list overlay** rendered
  above the video. The video **keeps playing** behind a semi-transparent scrim. Selecting another
  channel swaps the ExoPlayer media source; overlay then closes.

### 7.4 Movies
- Poster grid (`LazyVerticalGrid`). Row click → detail screen (poster, plot, rating, release).
- Detail → "Reproducir" → `PlayerScreen` with `{base}/movie/{u}/{p}/{id}.mp4`.

### 7.5 Series
- Poster grid → detail with season selector → episode list → play
  `{base}/series/{u}/{p}/{episode_stream_id}.m3u8`.

### 7.6 Search
- Query input → searches live names + movie names + series names (client-side filter over
  fetched catalogs). Reuses channel rows / poster cards.

### 7.7 Favorites
- Star (★) toggle on channels/movies/series stored in DataStore. Favorites screen lists them.

### 7.8 PlayerScreen
- Fullscreen ExoPlayer surface. TV controls: play/pause, seek (±), timeline, buffered state.
- Adaptive HLS quality (ExoPlayer default track selection).
- Back exits to previous screen. (On Live screen, Back also closes the overlay first.)

## 8. Player (Media3 ExoPlayer)

- `TvPlayer.prepare(hlsUrl, context)`: `ExoPlayer.Builder`, `DefaultMediaSourceFactory` with
  `HlsMediaSource.Factory`, `PlayerView` bound in Compose via `AndroidView`.
- Handles `.m3u8` directly (HLS). No manual segment handling (server proxies/segments).
- Lifecycle: create on enter, `release()` on dispose to avoid leaks.
- Errors (`PlaybackException`): surface retry button + friendly message; for live, allow
  "retry / next channel".

## 9. Error handling & states

- Loading / Empty / Error composables per screen.
- Network failures: retry with exponential backoff (small). Show message, keep last good data.
- Auth failure (401 / `auth==0`): route back to Login, clear stored creds.
- Stream play failure: per-player retry; for Live, suggest next channel.

## 10. Deliverables & build

- Complete Android Studio project: `app/`, `build.gradle.kts` (project + module),
  `settings.gradle.kts`, `gradle/wrapper`, `AndroidManifest.xml`, `res/` (banner, strings),
  `src/main/java/...` Kotlin source as structured in §5.
- `README.md` with: prerequisites (Android Studio, minSdk 21, Google TV / Android TV device or
  emulator with TV image), how to open, set base URL, build & run, sideload to Google TV.
- No build executed in this environment.

## 11. Acceptance criteria

1. App installs on Android TV and appears in the Leanback launcher.
2. Login with host/user/pass succeeds against the server; invalid creds show error.
3. Live TV shows enumerated channels; moving focus auto-plays; number/name jump works.
4. Clicking the player goes fullscreen; clicking in fullscreen shows channel overlay; selecting
   another channel swaps source while video continues.
5. Movies and Series browse, open detail, and play via ExoPlayer.
6. Search filters by name; Favorites persist across app restarts.
7. Credentials and favorites persist in DataStore; nothing hardcoded.
8. Player handles HLS errors with retry rather than crashing.
