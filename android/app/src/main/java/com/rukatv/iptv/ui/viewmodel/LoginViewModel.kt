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

    fun logout() {
        viewModelScope.launch {
            store.clear()
            _state.value = LoginUiState()
        }
    }

    private fun MutableStateFlow<LoginUiState>.update(f: (LoginUiState) -> LoginUiState) {
        value = f(value)
    }
}
