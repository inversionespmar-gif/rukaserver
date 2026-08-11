package com.rukatv.iptv

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukatv.iptv.data.local.CredentialsStore
import com.rukatv.iptv.data.local.FavoritesStore
import com.rukatv.iptv.data.local.PlaybackProgressStore
import com.rukatv.iptv.data.repository.AuthRepository
import com.rukatv.iptv.data.repository.CatalogRepository
import com.rukatv.iptv.data.repository.FavoritesRepository
import com.rukatv.iptv.data.repository.UpdateInfo
import com.rukatv.iptv.data.repository.UpdateRepository
import com.rukatv.iptv.ui.components.UpdateDialog
import com.rukatv.iptv.ui.screens.HomeScreen
import com.rukatv.iptv.ui.screens.LoginScreen
import com.rukatv.iptv.ui.screens.PlayerScreen
import com.rukatv.iptv.ui.theme.isTvDevice
import com.rukatv.iptv.ui.theme.phoneColorScheme
import com.rukatv.iptv.ui.theme.tvColorScheme
import com.rukatv.iptv.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val credsStore = CredentialsStore(this)
        val favStore = FavoritesStore(this)
        val progressStore = PlaybackProgressStore(this)
        val tv = isTvDevice(this)

        setContent {
            MaterialTheme(colorScheme = if (tv) tvColorScheme() else phoneColorScheme()) {
                AppContent(credsStore, favStore, progressStore, tv)
            }
        }
    }
}

@Composable
private fun AppContent(credsStore: CredentialsStore, favStore: FavoritesStore, progressStore: PlaybackProgressStore, isTv: Boolean) {
    val context = LocalContext.current
    var playerQueue by remember { mutableStateOf<List<PlayItem>?>(null) }
    var playerStart by remember { mutableStateOf(0) }
    var playerIsSeries by remember { mutableStateOf(false) }
    val loginVm = remember {
        LoginViewModel(credsStore) { host -> AuthRepository.buildApi(host) }
    }
    val loginState by loginVm.state.collectAsStateWithLifecycle()

    val updateRepo = remember { UpdateRepository() }
    var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    // OTA Update Check when logged in
    LaunchedEffect(loginState.loggedIn) {
        val creds = loginState.loggedIn ?: return@LaunchedEffect
        val currentVersionCode = runCatching {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode.toInt() else pkg.versionCode
        }.getOrDefault(1)

        val update = updateRepo.checkUpdate(creds.host, currentVersionCode)
        if (update != null) {
            pendingUpdate = update
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    catalog = catalog,
                    favorites = favorites,
                    progressStore = progressStore,
                    isTv = isTv,
                    onLogout = { loginVm.logout() },
                    onPlay = { url, title -> playerQueue = listOf(PlayItem(url, title)); playerStart = 0; playerIsSeries = false },
                    onPlayQueue = { items, start -> playerQueue = items; playerStart = start; playerIsSeries = true }
                )
            }
            else -> {
                LoginScreen(loginVm) { }
            }
        }

        // OTA Update Dialog
        if (pendingUpdate != null) {
            val updateInfo = pendingUpdate!!
            UpdateDialog(
                updateInfo = updateInfo,
                isDownloading = isDownloadingUpdate,
                downloadProgress = downloadProgress,
                onConfirmUpdate = {
                    isDownloadingUpdate = true
                    scope.launch {
                        updateRepo.downloadAndInstallApk(context, updateInfo.apkUrl) { progress ->
                            downloadProgress = progress
                        }
                        isDownloadingUpdate = false
                    }
                },
                onDismiss = {
                    pendingUpdate = null
                }
            )
        }
    }
}

