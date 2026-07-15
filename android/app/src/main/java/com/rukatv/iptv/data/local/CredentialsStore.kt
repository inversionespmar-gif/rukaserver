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
