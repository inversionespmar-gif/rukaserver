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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
