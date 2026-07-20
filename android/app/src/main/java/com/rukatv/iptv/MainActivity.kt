package com.rukatv.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.local.CredentialsStore
import com.rukatv.iptv.data.local.FavoritesStore
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.repository.AuthRepository
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.PlayItem
import com.rukatv.iptv.ui.screens.HomeScreen
import com.rukatv.iptv.ui.screens.LoginScreen
import com.rukatv.iptv.ui.screens.PlayerScreen
import com.rukatv.iptv.ui.theme.isTvDevice
import com.rukatv.iptv.ui.theme.phoneColorScheme
import com.rukatv.iptv.ui.theme.tvColorScheme
import com.rukatv.iptv.ui.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val credsStore = CredentialsStore(this)
        val favStore = FavoritesStore(this)
        val tv = isTvDevice(this)

        setContent {
            MaterialTheme(colorScheme = if (tv) tvColorScheme() else phoneColorScheme()) {
                AppContent(credsStore, favStore, tv)
            }
        }
    }
}

@Composable
private fun AppContent(credsStore: CredentialsStore, favStore: FavoritesStore, isTv: Boolean) {
    var playerQueue by remember { mutableStateOf<List<PlayItem>?>(null) }
    var playerStart by remember { mutableStateOf(0) }
    var playerIsSeries by remember { mutableStateOf(false) }
    val progressStore = remember { PlaybackProgressStore(applicationContext) }
    val loginVm = remember {
        LoginViewModel(credsStore) { host -> AuthRepository.buildApi(host) }
    }
    val loginState by loginVm.state.collectAsStateWithLifecycle()

    when {
        playerQueue != null -> {
            PlayerScreen(playerQueue!!, playerStart, playerIsSeries, progressStore) {
                playerQueue = null
                playerIsSeries = false
            }
        }
        loginState.loggedIn != null -> {
            val creds = loginState.loggedIn!!
            val api = remember(creds.host) { AuthRepository.buildApi(creds.host) }
            val catalog = remember(creds.host) { CatalogRepository(api, creds) }
            val favorites = remember { FavoritesRepository(favStore) }
            HomeScreen(
                catalog,
                favorites,
                isTv,
                onPlay = { url, title -> playerQueue = listOf(PlayItem(url, title)); playerStart = 0; playerIsSeries = false },
                onPlayQueue = { items, start -> playerQueue = items; playerStart = start; playerIsSeries = true }
            )
        }
        else -> {
            LoginScreen(loginVm) { }
        }
    }
}
