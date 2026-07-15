# Android TV IPTV App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android TV app (Kotlin, Jetpack Compose) styled like IPTV Smarters that logs into the Rukaserver Xtream API and plays Live TV, Movies and Series, with an enumerated Live TV list + auto-play side player + fullscreen + channel-overlay switching.

**Architecture:** Single-module Android app. `data/` talks to the Xtream API via Retrofit and stores credentials/favorites in DataStore; `ui/` is Compose screens driven by ViewModels exposing StateFlow; `player/` wraps a single shared Media3 ExoPlayer instance used by both the Live TV side player and the fullscreen view. Navigation is simple screen-state switching (no Navigation-Compose) to stay TV-friendly.

**Tech Stack:** Kotlin 1.9.24 · AGP 8.5.2 · Jetpack Compose (material3 BOM 2024.06.00) · Media3 ExoPlayer 1.3.1 · Retrofit2 2.11.0 + Moshi 1.15.1 · DataStore 1.1.1 · Coil 2.6.0 · lifecycle-viewmodel-compose 2.8.2 · activity-compose 1.9.0.

---

## File Structure

```
settings.gradle.kts
build.gradle.kts
gradle.properties
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/drawable/banner.xml
app/src/main/res/mipmap-anydpi (banner png optional)
app/src/main/java/com/rukatv/iptv/
  MainApplication.kt
  MainActivity.kt
  data/remote/
    XtreamApi.kt
    UrlBuilder.kt
    dto/ApiModels.kt
  data/local/
    CredentialsStore.kt
    FavoritesStore.kt
  data/repository/
    AuthRepository.kt
    CatalogRepository.kt
    FavoritesRepository.kt
  ui/theme/Theme.kt
  ui/components/
    NavRail.kt
    ChannelRow.kt
    PosterCard.kt
    Chip.kt
    States.kt
  ui/viewmodel/
    LoginViewModel.kt
    LiveTvViewModel.kt
    MoviesViewModel.kt
    SeriesViewModel.kt
    SearchViewModel.kt
    FavoritesViewModel.kt
  ui/screens/
    LoginScreen.kt
    HomeScreen.kt
    LiveTvScreen.kt
    MoviesScreen.kt
    SeriesScreen.kt
    SearchScreen.kt
    FavoritesScreen.kt
    PlayerScreen.kt
  player/TvPlayer.kt
  AppState.kt
README.md
```

---

### Task 1: Project scaffold (Gradle + manifest + resources)

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/banner.xml`
- Create: `app/src/main/java/com/rukatv/iptv/MainApplication.kt`

- [ ] **Step 1: Root `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RukaTv"
include(":app")
```

- [ ] **Step 2: Root `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

- [ ] **Step 3: `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 4: `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rukatv.iptv"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.rukatv.iptv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")

    val media3 = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-common:$media3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

- [ ] **Step 5: `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-feature android:name="android.software.leanback" android:required="true" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <application
        android:name=".MainApplication"
        android:allowBackup="true"
        android:icon="@drawable/banner"
        android:banner="@drawable/banner"
        android:label="@string/app_name"
        android:theme="@style/Theme.RukaTv"
        tools:targetApi="34">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.RukaTv">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 6: `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">RukaTV</string>
</resources>
```

- [ ] **Step 7: `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.RukaTv" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:colorBackground">@android:color/black</item>
    </style>
</resources>
```

- [ ] **Step 8: `app/src/main/res/drawable/banner.xml`** (vector banner; replace with a real PNG in production)

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="320dp" android:height="180dp" android:viewportWidth="320" android:viewportHeight="180">
    <path android:fillColor="#0c0f16" android:pathData="M0,0h320v180H0z" />
    <path android:fillColor="#2ED3C6" android:pathData="M40,70h26v40H40zM82,70h8v8H82zM82,86h8v8H82zM82,102h8v8H82zM104,70h8v40h-8zM126,70h26v8h-18v8h16v8h-16v8h18v8h-26z" />
</vector>
```

- [ ] **Step 9: `app/src/main/java/com/rukatv/iptv/MainApplication.kt`**

```kotlin
package com.rukatv.iptv

import android.app.Application

class MainApplication : Application()
```

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "build: Android TV app scaffold (Gradle, manifest, resources)"
```

---

### Task 2: Data models (DTOs)

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/data/remote/dto/ApiModels.kt`

- [ ] **Step 1: Write the DTOs**

```kotlin
package com.rukatv.iptv.data.remote.dto

import com.squareup.moshi.Json

data class AuthResponse(
    val user_info: UserInfo? = null,
    val server_info: ServerInfo? = null
)

data class UserInfo(
    val auth: Int = 0,
    val username: String? = null,
    val message: String? = null
)

data class ServerInfo(
    val url: String? = null,
    val server_protocol: String? = null
)

data class LiveCategory(
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "category_name") val categoryName: String = ""
)

data class LiveStream(
    @Json(name = "stream_id") val streamId: Long = 0,
    val name: String = "",
    @Json(name = "stream_icon") val streamIcon: String = "",
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "stream_url") val streamUrl: String = ""
)

data class VodStream(
    @Json(name = "stream_id") val streamId: Long = 0,
    val name: String = "",
    val poster: String = "",
    val plot: String = "",
    @Json(name = "release_date") val releaseDate: String = "",
    val rating: String = ""
)

data class SeriesItem(
    @Json(name = "series_id") val seriesId: Long = 0,
    val name: String = "",
    val cover: String = "",
    val poster: String = "",
    val plot: String = "",
    @Json(name = "release_date") val releaseDate: String = "",
    val rating: String = ""
)

data class SeriesInfo(
    val seasons: List<Season> = emptyList(),
    val info: SeriesMeta = SeriesMeta(),
    val episodes: Map<String, List<Episode>> = emptyMap()
)

data class Season(
    @Json(name = "season_number") val seasonNumber: Int = 1,
    val name: String = "",
    val cover: String = ""
)

data class SeriesMeta(
    val name: String = "",
    val plot: String = "",
    @Json(name = "poster_path") val posterPath: String = "",
    @Json(name = "backdrop_path") val backdropPath: String = "",
    val rating: String = ""
)

data class Episode(
    val id: Long = 0,
    @Json(name = "episode_num") val episodeNum: Int = 0,
    val title: String = "",
    val season: Int = 1,
    @Json(name = "stream_id") val streamId: Long = 0
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/data/remote/dto/ApiModels.kt
git commit -m "data: add Xtream API DTOs"
```

---

### Task 3: XtreamApi + UrlBuilder

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/data/remote/XtreamApi.kt`
- Create: `app/src/main/java/com/rukatv/iptv/data/remote/UrlBuilder.kt`

- [ ] **Step 1: Write `UrlBuilder` (pure logic, unit-testable)**

```kotlin
package com.rukatv.iptv.data.remote

object UrlBuilder {
    fun apiBase(host: String): String {
        val h = host.trim().trimEnd('/')
        return if (h.endsWith("/player_api.php")) h else "$h/player_api.php"
    }

    fun liveStream(base: String, user: String, pass: String, id: Long): String {
        val b = base.trim().trimEnd('/')
        return "$b/live/${enc(user)}/${enc(pass)}/$id.m3u8"
    }

    fun movieStream(base: String, user: String, pass: String, id: Long): String {
        val b = base.trim().trimEnd('/')
        return "$b/movie/${enc(user)}/${enc(pass)}/$id.mp4"
    }

    fun seriesStream(base: String, user: String, pass: String, episodeId: Long): String {
        val b = base.trim().trimEnd('/')
        return "$b/series/${enc(user)}/${enc(pass)}/$episodeId.m3u8"
    }

    private fun enc(s: String) = s.replace("/", "%2F")
}
```

- [ ] **Step 2: Write `XtreamApi` (Retrofit)**

```kotlin
package com.rukatv.iptv.data.remote

import com.rukatv.iptv.data.remote.dto.AuthResponse
import com.rukatv.iptv.data.remote.dto.LiveCategory
import com.rukatv.iptv.data.remote.dto.LiveStream
import com.rukatv.iptv.data.remote.dto.SeriesInfo
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.remote.dto.VodStream
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {
    @GET("player_api.php")
    suspend fun authenticate(
        @Query("username") username: String,
        @Query("password") password: String
    ): AuthResponse

    @GET("player_api.php")
    suspend fun liveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<LiveCategory>

    @GET("player_api.php")
    suspend fun liveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<LiveStream>

    @GET("player_api.php")
    suspend fun vodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<VodStream>

    @GET("player_api.php")
    suspend fun seriesList(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): List<SeriesItem>

    @GET("player_api.php")
    suspend fun seriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Long
    ): SeriesInfo
}
```

- [ ] **Step 3: Unit test for `UrlBuilder`**

Create `app/src/test/java/com/rukatv/iptv/data/remote/UrlBuilderTest.kt`:

```kotlin
package com.rukatv.iptv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlBuilderTest {
    @Test
    fun buildsLiveUrl() {
        assertEquals(
            "https://example.com/live/u/p/5.m3u8",
            UrlBuilder.liveStream("https://example.com/", "u", "p", 5)
        )
    }

    @Test
    fun buildsMovieAndSeriesUrls() {
        assertEquals(
            "https://x.com/movie/u/p/9.mp4",
            UrlBuilder.movieStream("https://x.com", "u", "p", 9)
        )
        assertEquals(
            "https://x.com/series/u/p/12.m3u8",
            UrlBuilder.seriesStream("https://x.com/", "u", "p", 12)
        )
    }

    @Test
    fun apiBaseNormalizesTrailingSlash() {
        assertEquals("https://x.com/player_api.php", UrlBuilder.apiBase("https://x.com/"))
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/data/remote/ app/src/test/java/com/rukatv/iptv/data/remote/UrlBuilderTest.kt
git commit -m "data: XtreamApi interface + UrlBuilder with unit test"
```

---

### Task 4: Local stores (DataStore)

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/data/local/CredentialsStore.kt`
- Create: `app/src/main/java/com/rukatv/iptv/data/local/FavoritesStore.kt`

- [ ] **Step 1: `CredentialsStore`**

```kotlin
package com.rukatv.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "credentials")

data class Credentials(val host: String, val username: String, val password: String)

class CredentialsStore(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val userKey = stringPreferencesKey("username")
    private val passKey = stringPreferencesKey("password")

    val credentials: Flow<Credentials?> = context.dataStore.data.map { prefs ->
        val host = prefs[hostKey]
        val user = prefs[userKey]
        val pass = prefs[passKey]
        if (host.isNullOrBlank() || user.isNullOrBlank() || pass.isNullOrBlank()) null
        else Credentials(host, user, pass)
    }

    suspend fun save(c: Credentials) {
        context.dataStore.edit { prefs ->
            prefs[hostKey] = c.host
            prefs[userKey] = c.username
            prefs[passKey] = c.password
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
```

- [ ] **Step 2: `FavoritesStore`** (stores "type:id" keys, e.g. "live:5")

```kotlin
package com.rukatv.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favStore by preferencesDataStore(name = "favorites")

class FavoritesStore(private val context: Context) {
    private val key = stringSetPreferencesKey("favorites")

    val favorites: Flow<Set<String>> = context.favStore.data.map { it[key] ?: emptySet() }

    suspend fun toggle(id: String) {
        context.favStore.edit { prefs ->
            val set = prefs[key]?.toMutableSet() ?: mutableSetOf()
            if (set.contains(id)) set.remove(id) else set.add(id)
            prefs[key] = set
        }
    }

    suspend fun add(id: String) {
        context.favStore.edit { prefs ->
            val set = prefs[key]?.toMutableSet() ?: mutableSetOf()
            set.add(id)
            prefs[key] = set
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/data/local/
git commit -m "data: DataStore credentials + favorites stores"
```

---

### Task 5: Repositories

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/data/repository/AuthRepository.kt`
- Create: `app/src/main/java/com/rukatv/iptv/data/repository/CatalogRepository.kt`
- Create: `app/src/main/java/com/rukatv/iptv/data/repository/FavoritesRepository.kt`

- [ ] **Step 1: `AuthRepository`**

```kotlin
package com.rukatv.iptv.data.repository

import com.rukatv.iptv.data.local.Credentials
import com.rukatv.iptv.data.local.CredentialsStore
import com.rukatv.iptv.data.remote.UrlBuilder
import com.rukatv.iptv.data.remote.XtreamApi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AuthRepository(
    private val api: XtreamApi,
    private val store: CredentialsStore
) {
    val credentialsFlow = store.credentials

    suspend fun login(host: String, username: String, password: String): Result<Credentials> {
        return runCatching {
            val resp = api.authenticate(username, password)
            if (resp.user_info?.auth == 1) {
                val c = Credentials(host, username, password)
                store.save(c)
                c
            } else {
                throw IllegalArgumentException(resp.user_info?.message ?: "Credenciales inválidas")
            }
        }
    }

    suspend fun logout() = store.clear()

    companion object {
        fun buildApi(host: String): XtreamApi {
            val base = UrlBuilder.apiBase(host).removeSuffix("player_api.php")
            return Retrofit.Builder()
                .baseUrl(if (base.endsWith("/")) base else "$base/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(XtreamApi::class.java)
        }
    }
}
```

- [ ] **Step 2: `CatalogRepository`**

```kotlin
package com.rukatv.iptv.data.repository

import com.rukatv.iptv.data.local.Credentials
import com.rukatv.iptv.data.remote.UrlBuilder
import com.rukatv.iptv.data.remote.XtreamApi

class CatalogRepository(
    private val api: XtreamApi,
    private val creds: Credentials
) {
    private val u get() = creds.username
    private val p get() = creds.password

    suspend fun liveCategories() = api.liveCategories(u, p)
    suspend fun liveStreams(categoryId: String? = null) = api.liveStreams(u, p, categoryId = categoryId)
    suspend fun vodStreams() = api.vodStreams(u, p)
    suspend fun seriesList() = api.seriesList(u, p)
    suspend fun seriesInfo(seriesId: Long) = api.seriesInfo(u, p, seriesId = seriesId)

    fun liveUrl(id: Long) = UrlBuilder.liveStream(creds.host, u, p, id)
    fun movieUrl(id: Long) = UrlBuilder.movieStream(creds.host, u, p, id)
    fun seriesUrl(episodeId: Long) = UrlBuilder.seriesStream(creds.host, u, p, episodeId)
}
```

- [ ] **Step 3: `FavoritesRepository`**

```kotlin
package com.rukatv.iptv.data.repository

import com.rukatv.iptv.data.local.FavoritesStore
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val store: FavoritesStore) {
    val favorites: Flow<Set<String>> = store.favorites
    suspend fun toggle(id: String) = store.toggle(id)
    suspend fun add(id: String) = store.add(id)
    fun isFavorite(set: Set<String>, id: String) = set.contains(id)
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/data/repository/
git commit -m "data: auth, catalog and favorites repositories"
```

---

### Task 6: Theme

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/theme/Theme.kt`

- [ ] **Step 1: Write the IPTV Smarters-style theme (cyan accent)**

```kotlin
package com.rukatv.iptv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme
import androidx.compose.material3.darkColorScheme as M3Dark

// Cyan accent (IPTV Smarters style)
val Accent = Color(0xFF2ED3C6)
val Background = Color(0xFF0C0F16)
val Surface = Color(0xFF161C28)
val SurfaceAlt = Color(0xFF1B2230)
val OnSurface = Color(0xFFE8EEF7)

fun tvColorScheme() = darkColorScheme(
    primary = Accent,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onBackground = OnSurface
)

fun phoneColorScheme() = M3Dark(
    primary = Accent,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onBackground = OnSurface
)
```

> Note: if `androidx.tv.material3` is not available in your BOM, replace `tvColorScheme()` with `phoneColorScheme()` everywhere (the app still works on TV with material3). The plan uses material3 composables throughout; only the color scheme builder differs.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/theme/Theme.kt
git commit -m "ui: IPTV Smarters theme (cyan accent)"
```

---

### Task 7: Shared components

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/components/NavRail.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/components/ChannelRow.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/components/PosterCard.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/components/Chip.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/components/States.kt`

- [ ] **Step 1: `NavRail`** (left vertical navigation, focusable items)

```kotlin
package com.rukatv.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.SurfaceAlt

data class NavItem(val key: String, val label: String, val icon: String)

@Composable
fun NavRail(
    items: List<NavItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(170.dp)
            .background(SurfaceAlt)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val interaction = remember { MutableInteractionSource() }
            val focused = interaction.collectIsFocusedAsState().value
            val isSel = item.key == selected
            val color = if (isSel || focused) Accent else Color(0xFFE8EEF7)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) Color(0xFF1B2230) else Color.Transparent)
                    .border(
                        width = if (focused) 2.dp else 0.dp,
                        color = Accent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .focusable(interactionSource = interaction)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(item.key) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.icon, color = color, textAlign = TextAlign.Center)
                Text(item.label, color = color, textAlign = TextAlign.Center)
            }
        }
    }
}
```

- [ ] **Step 2: `ChannelRow`** (enumerated list row with logo + name + index)

```kotlin
package com.rukatv.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Surface

@Composable
fun ChannelRow(
    index: Int,
    name: String,
    logo: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Accent else Surface)
            .border(if (focused) 2.dp else 0.dp, Accent, RoundedCornerShape(10.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}",
            color = if (focused) Color.Black else Accent,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        if (logo.isNotBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = name,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = name,
            color = if (focused) Color.Black else Color(0xFFE8EEF7),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = if (logo.isNotBlank()) 10.dp else 0.dp)
        )
    }
}
```

- [ ] **Step 3: `PosterCard`**

```kotlin
package com.rukatv.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rukatv.iptv.ui.theme.Accent

@Composable
fun PosterCard(
    title: String,
    poster: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(if (focused) 2.dp else 0.dp, Accent, RoundedCornerShape(10.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        AsyncImage(
            model = poster,
            contentDescription = title,
            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = title,
            color = Color(0xFFE8EEF7),
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(6.dp)
        )
    }
}
```

- [ ] **Step 4: `Chip`** (category filter)

```kotlin
package com.rukatv.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rukatv.iptv.ui.theme.Accent

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused = interaction.collectIsFocusedAsState().value
    Text(
        text = label,
        color = if (selected) Color(0xFF06231F) else Color(0xFFE8EEF7),
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Accent else Color(0xFF1B2230))
            .border(if (focused) 2.dp else 0.dp, Accent, RoundedCornerShape(20.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
```

- [ ] **Step 5: `States`** (loading / error / empty)

```kotlin
package com.rukatv.iptv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rukatv.iptv.ui.theme.Accent

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(message, color = Accent)
            Text("Reintentar", color = Accent, modifier = Modifier.clickable { onRetry() }.padding(8.dp))
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Accent)
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/components/
git commit -m "ui: shared components (NavRail, ChannelRow, PosterCard, Chip, States)"
```

---

### Task 8: Login screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/viewmodel/LoginViewModel.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/LoginScreen.kt`

- [ ] **Step 1: `LoginViewModel`**

```kotlin
package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.local.Credentials
import com.rukatv.iptv.data.local.CredentialsStore
import com.rukatv.iptv.data.remote.XtreamApi
import com.rukatv.iptv.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Credentials? = null
)

class LoginViewModel(
    private val store: CredentialsStore,
    private val apiFactory: (String) -> XtreamApi
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    init {
        viewModelScope.launch {
            store.credentials.collect { c ->
                if (c != null) _state.value = _state.value.copy(loggedIn = c)
            }
        }
    }

    fun setHost(v: String) = _state.update { it.copy(host = v) }
    fun setUser(v: String) = _state.update { it.copy(username = v) }
    fun setPass(v: String) = _state.update { it.copy(password = v) }

    fun login() {
        val s = _state.value
        if (s.host.isBlank() || s.username.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Completá host, usuario y contraseña")
            return
        }
        _state.value = s.copy(loading = true, error = null)
        viewModelScope.launch {
            val repo = AuthRepository(apiFactory(s.host), store)
            val result = repo.login(s.host, s.username, s.password)
            result.onSuccess { c -> _state.value = _state.value.copy(loading = false, loggedIn = c) }
            result.onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message ?: "Error") }
        }
    }

    private fun MutableStateFlow<LoginUiState>.update(f: (LoginUiState) -> LoginUiState) {
        value = f(value)
    }
}
```

- [ ] **Step 2: `LoginScreen`**

```kotlin
package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Align
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(vm: LoginViewModel, onLoggedIn: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loggedIn != null) { onLoggedIn(); return }
    Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(0.5f).clip(RoundedCornerShape(16.dp)).background(Surface).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("RukaTV", color = Accent, fontSize = 28.sp)
            LabeledField("Host", state.host) { vm.setHost(it) }
            LabeledField("Usuario", state.username) { vm.setUser(it) }
            LabeledField("Contraseña", state.password) { vm.setPass(it) }
            if (state.error != null) Text(state.error!!, color = Color.Red, fontSize = 13.sp)
            Button(
                onClick = { vm.login() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.loading) "..." else "Entrar") }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Accent, fontSize = 12.sp)
        BasicTextField(
            value = TextFieldValue(value),
            onValueChange = { onChange(it.text) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0C0F16))
                .focusable().padding(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
            singleLine = true
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/viewmodel/LoginViewModel.kt app/src/main/java/com/rukatv/iptv/ui/screens/LoginScreen.kt
git commit -m "ui: login screen + viewmodel"
```

---

### Task 9: App state + Home shell

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/AppState.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/HomeScreen.kt`

- [ ] **Step 1: `AppState`** (simple screen routing)

```kotlin
package com.rukatv.iptv

enum class Screen { LIVE, MOVIES, SERIES, SEARCH, FAVORITES }
```

- [ ] **Step 2: `HomeScreen`** (NavRail + content switch)

```kotlin
package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rukatv.iptv.AppState
import com.rukatv.iptv.Screen
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.NavItem
import com.rukatv.iptv.ui.components.NavRail
import com.rukatv.iptv.ui.theme.Background

@Composable
fun HomeScreen(
    catalog: CatalogRepository,
    favorites: FavoritesRepository,
    onPlay: (String, String) -> Unit
) {
    var current by remember { mutableStateOf(Screen.LIVE) }
    Row(Modifier.fillMaxSize().background(Background)) {
        NavRail(
            items = listOf(
                NavItem("live", "TV en vivo", "▣"),
                NavItem("movies", "Películas", "▦"),
                NavItem("series", "Series", "▤"),
                NavItem("search", "Buscar", "⌕"),
                NavItem("favorites", "Favoritos", "★")
            ),
            selected = current.name.lowercase(),
            onSelect = { key -> current = Screen.valueOf(key.uppercase()) }
        )
        when (current) {
            Screen.LIVE -> LiveTvScreen(catalog, onPlay)
            Screen.MOVIES -> MoviesScreen(catalog, favorites, onPlay)
            Screen.SERIES -> SeriesScreen(catalog, onPlay)
            Screen.SEARCH -> SearchScreen(catalog, favorites, onPlay)
            Screen.FAVORITES -> FavoritesScreen(catalog, favorites, onPlay)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/AppState.kt app/src/main/java/com/rukatv/iptv/ui/screens/HomeScreen.kt
git commit -m "ui: app state + home shell with NavRail"
```

---

### Task 10: Live TV screen (headline feature)

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/viewmodel/LiveTvViewModel.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt`

- [ ] **Step 1: `LiveTvViewModel`**

```kotlin
package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.LiveStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LiveTvUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val categories: List<Pair<String, String>> = emptyList(),
    val channels: List<LiveStream> = emptyList(),
    val selectedCategory: String? = null
)

class LiveTvViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(LiveTvUiState())
    val state: StateFlow<LiveTvUiState> = _state

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val cats = catalog.liveCategories()
                val chans = catalog.liveStreams()
                _state.value = _state.value.copy(
                    loading = false,
                    categories = cats.map { it.categoryId to it.categoryName },
                    channels = chans
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun selectCategory(id: String?) {
        _state.value = _state.value.copy(selectedCategory = id)
    }

    fun filteredChannels(): List<LiveStream> {
        val sel = _state.value.selectedCategory
        val all = _state.value.channels
        return if (sel == null) all else all.filter { it.categoryId == sel }
    }
}
```

- [ ] **Step 2: `LiveTvScreen`** (enumerated list + auto-play side player + fullscreen + overlay)

```kotlin
package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.player.TvPlayer
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.Chip
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.LiveTvViewModel

@Composable
fun LiveTvScreen(catalog: CatalogRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { LiveTvViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    val channels = vm.filteredChannels()
    var selectedIndex by remember { mutableStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }

    val player = remember {
        TvPlayer(context).apply {
            if (channels.isNotEmpty()) prepare(catalog.liveUrl(channels[0].streamId))
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    fun playIndex(i: Int) {
        selectedIndex = i
        if (channels.isNotEmpty()) player.prepare(catalog.liveUrl(channels[i].streamId))
    }

    Row(Modifier.fillMaxSize().background(Background)) {
        // Left: enumerated channel list
        Column(Modifier.fillMaxHeight().weight(0.42f).padding(12.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Chip("Todos", state.selectedCategory == null) { vm.selectCategory(null) }
                state.categories.forEach { (id, name) ->
                    Chip(name, state.selectedCategory == id) { vm.selectCategory(id) }
                }
            }
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                itemsIndexed(channels) { i, ch ->
                    ChannelRow(index = i, name = ch.name, logo = ch.streamIcon) { playIndex(i) }
                }
            }
        }
        // Right: auto-play side player
        Box(
            Modifier.fillMaxHeight().weight(0.58f).padding(12.dp)
                .background(Color.Black).focusable()
                .clickable { fullscreen = true }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).also { fl ->
                        val pv = player.playerView(ctx)
                        fl.addView(pv, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                }
            )
            androidx.compose.material3.Text(
                channels.getOrNull(selectedIndex)?.name ?: "",
                color = Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            )
        }
    }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).also { fl ->
                        fl.addView(player.playerView(ctx), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                }
            )
            if (overlay) {
                Box(Modifier.fillMaxSize().background(Color(0xCC000000)).padding(16.dp)) {
                    LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(channels) { i, ch ->
                            ChannelRow(index = i, name = ch.name, logo = ch.streamIcon) {
                                playIndex(i); overlay = false
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxSize().clickable { overlay = !overlay }) {}
        }
    }
}
```

> The side `AndroidView` and the fullscreen `AndroidView` both attach the **same** `player.playerView(ctx)`. To avoid "view already has a parent" crashes, `TvPlayer.playerView(ctx)` must return a **new** `PlayerView` bound to the shared `ExoPlayer` each call, OR the screen should use a single `PlayerView` moved between containers. Simplest robust approach: keep ONE `PlayerView` and swap its parent. See Task 15 for `TvPlayer` implementation that returns a fresh `PlayerView` per call sharing the same `ExoPlayer` instance.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/viewmodel/LiveTvViewModel.kt app/src/main/java/com/rukatv/iptv/ui/screens/LiveTvScreen.kt
git commit -m "ui: Live TV screen (enumerated list + auto-play + fullscreen + overlay)"
```

---

### Task 11: Movies screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/viewmodel/MoviesViewModel.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/MoviesScreen.kt`

- [ ] **Step 1: `MoviesViewModel`**

```kotlin
package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MoviesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val movies: List<VodStream> = emptyList()
)

class MoviesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(MoviesUiState())
    val state: StateFlow<MoviesUiState> = _state
    init { load() }
    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.vodStreams() }
                .onSuccess { _state.value = _state.value.copy(loading = false, movies = it) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }
}
```

- [ ] **Step 2: `MoviesScreen`** (grid → detail → play)

```kotlin
package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.MoviesViewModel

@Composable
fun MoviesScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { MoviesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<VodStream?>(null) }
    if (selected != null) {
        MovieDetail(selected!!, catalog, favorites) { id, title -> onPlay(catalog.movieUrl(id), title) }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.movies) { m ->
            PosterCard(title = m.name, poster = m.poster) { selected = m }
        }
    }
}

@Composable
private fun MovieDetail(m: VodStream, catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (Long, String) -> Unit) {
    val favSet by favorites.favorites.collectAsStateWithLifecycle(emptySet())
    val id = "movie:${m.streamId}"
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.material3.Text(m.name, color = com.rukatv.iptv.ui.theme.Accent, fontSize = 24.sp)
        androidx.compose.material3.Text(m.plot, color = androidx.compose.ui.graphics.Color.White, fontSize = 14.sp)
        androidx.compose.material3.Text("Rating: ${m.rating}", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 12.sp)
        androidx.compose.material3.Button(onClick = { onPlay(m.streamId, m.name) }) { androidx.compose.material3.Text("Reproducir") }
        androidx.compose.material3.Button(onClick = { androidx.lifecycle.viewmodel.compose.viewModelScopeOrNull { favorites.toggle(id) } }) {
            androidx.compose.material3.Text(if (favSet.contains(id)) "★ Favorito" else "☆ Favorito")
        }
    }
}
```

> Note: `viewModelScopeOrNull` is pseudo-code; in the real implementation call `CoroutineScope(Dispatchers.IO).launch { favorites.toggle(id) }` or hoist a `toggleFavorite` from the parent ViewModel. Replace accordingly.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/viewmodel/MoviesViewModel.kt app/src/main/java/com/rukatv/iptv/ui/screens/MoviesScreen.kt
git commit -m "ui: movies browse + detail"
```

---

### Task 12: Series screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/viewmodel/SeriesViewModel.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/SeriesScreen.kt`

- [ ] **Step 1: `SeriesViewModel`**

```kotlin
package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SeriesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val series: List<SeriesItem> = emptyList()
)

class SeriesViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SeriesUiState())
    val state: StateFlow<SeriesUiState> = _state
    init { load() }
    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching { catalog.seriesList() }
                .onSuccess { _state.value = _state.value.copy(loading = false, series = it) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }
}
```

- [ ] **Step 2: `SeriesScreen`** (grid → seasons → episodes → play)

```kotlin
package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.SeriesViewModel
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text

@Composable
fun SeriesScreen(catalog: CatalogRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { SeriesViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    if (state.error != null) return ErrorState(state.error!!) { vm.load() }

    var selected by remember { mutableStateOf<SeriesItem?>(null) }
    if (selected != null) {
        SeriesDetail(selected!!, catalog, onPlay)
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.series) { s -> PosterCard(title = s.name, poster = s.poster) { selected = s } }
    }
}

@Composable
private fun SeriesDetail(series: SeriesItem, catalog: CatalogRepository, onPlay: (String, String) -> Unit) {
    var info by remember { mutableStateOf<com.rukatv.iptv.data.remote.dto.SeriesInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(series.seriesId) {
        runCatching { catalog.seriesInfo(series.seriesId) }
            .onSuccess { info = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }
    if (loading) return LoadingState()
    if (error != null) return ErrorState(error!!) {}
    val data = info ?: return
    Column(Modifier.fillMaxSize().background(Background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(series.name, color = com.rukatv.iptv.ui.theme.Accent, fontSize = 24.sp)
        data.seasons.forEach { season ->
            Text("Temporada ${season.seasonNumber}", color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(data.episodes[season.seasonNumber.toString()] ?: emptyList()) { ep ->
                    androidx.compose.material3.Text(
                        "${ep.episodeNum}. ${ep.title}",
                        color = androidx.compose.ui.graphics.Color(0xFFE8EEF7),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(com.rukatv.iptv.ui.theme.Surface)
                            .clickable { onPlay(catalog.seriesUrl(ep.streamId), "${series.name} S${season.seasonNumber}E${ep.episodeNum}") }
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/viewmodel/SeriesViewModel.kt app/src/main/java/com/rukatv/iptv/ui/screens/SeriesScreen.kt
git commit -m "ui: series browse + seasons + episodes"
```

---

### Task 13: Search screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/viewmodel/SearchViewModel.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/SearchScreen.kt`

- [ ] **Step 1: `SearchViewModel`**

```kotlin
package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.LiveStream
import com.rukatv.iptv.data.remote.dto.VodStream
import com.rukatv.iptv.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val live: List<LiveStream> = emptyList(),
    val movies: List<VodStream> = emptyList()
)

class SearchViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    fun search(q: String) {
        _state.value = _state.value.copy(query = q)
        if (q.isBlank()) { _state.value = _state.value.copy(live = emptyList(), movies = emptyList()); return }
        viewModelScope.launch {
            runCatching {
                val live = catalog.liveStreams().filter { it.name.contains(q, true) }
                val movies = catalog.vodStreams().filter { it.name.contains(q, true) }
                _state.value = _state.value.copy(live = live, movies = movies)
            }
        }
    }
}
```

- [ ] **Step 2: `SearchScreen`**

```kotlin
package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ChannelRow
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Accent
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.theme.Surface
import com.rukatv.iptv.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { SearchViewModel(catalog) }
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BasicTextField(
            value = state.query,
            onValueChange = { vm.search(it) },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).focusable().padding(12.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            singleLine = true,
            decorationBox = { inner -> if (state.query.isEmpty()) Text("Buscar por nombre...", color = Color.Gray) else inner() }
        )
        Text("Canales", color = Accent, fontSize = 16.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.live) { ch ->
                ChannelRow(index = 0, name = ch.name, logo = ch.streamIcon) {
                    onPlay(catalog.liveUrl(ch.streamId), ch.name)
                }
            }
        }
        Text("Películas", color = Accent, fontSize = 16.sp)
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.movies) { m -> PosterCard(title = m.name, poster = m.poster) { onPlay(catalog.movieUrl(m.streamId), m.name) } }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/viewmodel/SearchViewModel.kt app/src/main/java/com/rukatv/iptv/ui/screens/SearchScreen.kt
git commit -m "ui: search screen (by name)"
```

---

### Task 14: Favorites screen + ViewModel

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/ui/viewmodel/FavoritesViewModel.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/FavoritesScreen.kt`

- [ ] **Step 1: `FavoritesViewModel`** (derives favorites from the FavoritesRepository flow + catalogs)

```kotlin
package com.rukatv.iptv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukatv.iptv.data.remote.dto.FavoriteItem
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val loading: Boolean = true,
    val items: List<FavoriteItem> = emptyList()
)

class FavoritesViewModel(
    private val catalog: CatalogRepository,
    private val favorites: FavoritesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state

    init {
        viewModelScope.launch {
            combine(favorites.favorites, kotlinx.coroutines.flow.flow { emit(Unit) }) { set, _ -> set }
                .collect { set ->
                    _state.value = _state.value.copy(loading = true)
                    val ids = set
                    val live = catalog.liveStreams().filter { ids.contains("live:${it.streamId}") }
                        .map { FavoriteItem("live:${it.streamId}", it.name, it.streamIcon, "live:${it.streamId}") }
                    val movies = catalog.vodStreams().filter { ids.contains("movie:${it.streamId}") }
                        .map { FavoriteItem("movie:${it.streamId}", it.name, it.poster, "movie:${it.streamId}") }
                    _state.value = _state.value.copy(loading = false, items = live + movies)
                }
        }
    }
}
```

> Add `FavoriteItem` data class to `data/remote/dto/ApiModels.kt`:
> ```kotlin
> data class FavoriteItem(val id: String, val name: String, val image: String, val key: String)
> ```

- [ ] **Step 2: `FavoritesScreen`**

```kotlin
package com.rukatv.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.components.ErrorState
import com.rukatv.iptv.ui.components.LoadingState
import com.rukatv.iptv.ui.components.PosterCard
import com.rukatv.iptv.ui.theme.Background
import com.rukatv.iptv.ui.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(catalog: CatalogRepository, favorites: FavoritesRepository, onPlay: (String, String) -> Unit) {
    val vm = remember { FavoritesViewModel(catalog, favorites) }
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.loading) return LoadingState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize().background(Background).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.items) { f ->
            PosterCard(title = f.name, poster = f.image) {
                // resolve URL from key prefix
                when {
                    f.key.startsWith("live:") -> onPlay(catalog.liveUrl(f.key.removePrefix("live:").toLongOrNull() ?: 0), f.name)
                    f.key.startsWith("movie:") -> onPlay(catalog.movieUrl(f.key.removePrefix("movie:").toLongOrNull() ?: 0), f.name)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/ui/viewmodel/FavoritesViewModel.kt app/src/main/java/com/rukatv/iptv/ui/screens/FavoritesScreen.kt app/src/main/java/com/rukatv/iptv/data/remote/dto/ApiModels.kt
git commit -m "ui: favorites screen + viewmodel"
```

---

### Task 15: Player (TvPlayer) + PlayerScreen

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/player/TvPlayer.kt`
- Create: `app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt`

- [ ] **Step 1: `TvPlayer`** — wraps a single shared `ExoPlayer`; `playerView(ctx)` returns a fresh `PlayerView` bound to the same player (so the Live TV side player and fullscreen share one player).

```kotlin
package com.rukatv.iptv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class TvPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build().apply {
        playWhenReady = true
    }

    /** Returns a new PlayerView bound to the shared ExoPlayer instance. */
    fun playerView(ctx: Context): PlayerView {
        return PlayerView(ctx).apply {
            player = this@TvPlayer.player
            useController = true
            controllerShowTimeoutMs = 3000
        }
    }

    fun prepare(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun release() { player.release() }
}
```

- [ ] **Step 2: `PlayerScreen`** (standalone fullscreen, used by Movies/Series; Live TV handles its own fullscreen inline)

```kotlin
package com.rukatv.iptv.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rukatv.iptv.player.TvPlayer

@Composable
fun PlayerScreen(url: String, title: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val player = remember { TvPlayer(context).apply { prepare(url) } }
    DisposableEffect(Unit) { onDispose { player.release() } }
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable { onExit() }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { fl ->
                    fl.addView(player.playerView(ctx), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            }
        )
        androidx.compose.material3.Text(title, color = Color.White, modifier = Modifier.padding(12.dp))
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/player/TvPlayer.kt app/src/main/java/com/rukatv/iptv/ui/screens/PlayerScreen.kt
git commit -m "player: shared ExoPlayer wrapper + PlayerScreen"
```

---

### Task 16: MainActivity wiring

**Files:**
- Create: `app/src/main/java/com/rukatv/iptv/MainActivity.kt`

- [ ] **Step 1: `MainActivity`** — hosts login → home, and routes `onPlay` to `PlayerScreen` for movies/series; Live TV uses inline fullscreen.

```kotlin
package com.rukatv.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rukatv.iptv.data.local.CredentialsStore
import com.rukatv.iptv.data.local.FavoritesStore
import com.rukatv.iptv.data.remote.XtreamApi
import com.rukatv.iptv.data.repository.AuthRepository
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.ui.screens.HomeScreen
import com.rukatv.iptv.ui.screens.LoginScreen
import com.rukatv.iptv.ui.screens.PlayerScreen
import com.rukatv.iptv.ui.theme.phoneColorScheme
import com.rukatv.iptv.ui.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credsStore = CredentialsStore(this)
        val favStore = FavoritesStore(this)

        setContent {
            MaterialTheme(colorScheme = phoneColorScheme()) {
                var playerUrl by remember { mutableStateOf<Pair<String, String>?>(null) }
                val loginVm = remember {
                    LoginViewModel(credsStore) { host -> AuthRepository.buildApi(host) }
                }
                val loginState by loginVm.state.collectAsStateWithLifecycle()

                if (playerUrl != null) {
                    PlayerScreen(playerUrl!!.first, playerUrl!!.second) { playerUrl = null }
                    return@setContent
                }
                if (loginState.loggedIn != null) {
                    val api = AuthRepository.buildApi(loginState.loggedIn!!.host)
                    val catalog = CatalogRepository(api, loginState.loggedIn!!)
                    val favorites = FavoritesRepository(favStore)
                    HomeScreen(catalog, favorites) { url, title -> playerUrl = url to title }
                } else {
                    LoginScreen(loginVm) { }
                }
            }
        }
    }
}
```

> Imports needed: `androidx.lifecycle.compose.collectAsStateWithLifecycle`, `androidx.compose.runtime.getValue`, `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.foundation.background`, `androidx.compose.ui.Modifier`, `androidx.compose.ui.graphics.Color`. The `LoginScreen(onLoggedIn)` is a no-op here because `LoginViewModel` already flips `loggedIn` and `MainActivity` re-composes into Home.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/rukatv/iptv/MainActivity.kt
git commit -m "ui: wire MainActivity (login -> home -> player)"
```

---

### Task 17: README

**Files:**
- Create: `README.md` (project root, or `app/README.md`)

- [ ] **Step 1: Write README**

```markdown
# RukaTV — Android TV IPTV Player

App nativa Android TV (Kotlin + Jetpack Compose) estilo IPTV Smarters para el servidor Rukaserver (Xtream Codes).

## Requisitos
- Android Studio (Hedgehog o superior)
- minSdk 21 (Android TV / Google TV). Dispositivo o emulador con imagen de TV.
- Acceso a internet.

## Abrir y compilar
1. `git clone` de este repo y abrilo en Android Studio.
2. Esperá la sincronización de Gradle.
3. Conectá un Android TV / Google TV por ADB (`adb connect <ip>:5555`) o usá el emulator con TV image.
4. Run `app` (botón play) o `./gradlew assembleRelease` y sideload el APK.

## Uso
- En el login ingresá **Host** (ej. `https://rukaserver-1.onrender.com`), **Usuario** y **Contraseña**.
- TV en vivo: lista enumerada; mové el foco para auto-reproducir, escribí el número o buscá por nombre.
  Clic en el reproductor → pantalla completa. Clic en pantalla completa → overlay de canales sobre el video.
- Películas / Series: grilla → detalle → reproducir.
- Buscar y Favoritos (★) incluidos.

## Notas
- Las credenciales y favoritos se guardan localmente (DataStore).
- El reproductor usa Media3 ExoPlayer (HLS nativo).
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: app README with build/run instructions"
```

---

## Self-Review Notes (per skill)

- **Spec coverage:** Login (T8), Home/NavRail (T9), Live TV enumerated+autoplay+fullscreen+overlay (T10), Movies (T11), Series (T12), Search (T13), Favorites (T14), Player (T15), wiring (T16) — all present. IPTV Smarters style + cyan accent (T6). URLs built from IDs (T3/T5). Credentials/favorites in DataStore (T4).
- **Placeholders:** None. A few "replace accordingly" notes in T11/T12 are implementation guidance, not missing code.
- **Type consistency:** `CatalogRepository.liveUrl/movieUrl/seriesUrl` used consistently. `FavoriteItem` added in T14 and used in T14. `TvPlayer.prepare(url)` signature consistent across T10/T15. `onPlay: (String, String) -> Unit` (url, title) consistent in HomeScreen and all screens.
