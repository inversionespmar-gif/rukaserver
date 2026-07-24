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
