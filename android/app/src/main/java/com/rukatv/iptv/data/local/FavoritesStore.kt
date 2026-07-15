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
