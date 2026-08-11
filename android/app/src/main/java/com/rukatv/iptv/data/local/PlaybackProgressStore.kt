package com.rukatv.iptv.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rukatv.iptv.data.remote.dto.ContinueWatchingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.progressDataStore by preferencesDataStore(name = "playback_progress")

class PlaybackProgressStore(private val context: Context) {
    private val progressKey = stringPreferencesKey("progress")
    private val itemsKey = stringPreferencesKey("continue_watching_items")

    fun getProgress(url: String): Flow<Long?> =
        context.progressDataStore.data.map { prefs ->
            val json = prefs[progressKey]
            if (json.isNullOrBlank()) return@map null
            val obj = JSONObject(json)
            if (obj.has(url)) obj.getLong(url) else null
        }

    val continueWatchingList: Flow<List<ContinueWatchingItem>> =
        context.progressDataStore.data.map { prefs ->
            val json = prefs[itemsKey] ?: return@map emptyList()
            try {
                val array = JSONArray(json)
                val list = mutableListOf<ContinueWatchingItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ContinueWatchingItem(
                            streamId = obj.optLong("streamId", 0L),
                            title = obj.optString("title", ""),
                            poster = obj.optString("poster", ""),
                            positionMs = obj.optLong("positionMs", 0L),
                            durationMs = obj.optLong("durationMs", 0L),
                            isSeries = obj.optBoolean("isSeries", false),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
                list.sortedByDescending { it.lastUpdated }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun saveProgress(
        url: String,
        positionMs: Long,
        streamId: Long = 0,
        title: String = "",
        poster: String = "",
        durationMs: Long = 0,
        isSeries: Boolean = false
    ) {
        context.progressDataStore.edit { prefs ->
            // Save raw URL -> position map
            val json = prefs[progressKey]
            val obj = if (json.isNullOrBlank()) JSONObject() else JSONObject(json)
            obj.put(url, positionMs)
            prefs[progressKey] = obj.toString()

            // Extract numeric stream ID if omitted
            val effectiveStreamId = if (streamId > 0) streamId else extractIdFromUrl(url)

            // Save structured ContinueWatching item if title is provided
            if (title.isNotBlank()) {
                val rawItems = prefs[itemsKey]
                val array = if (!rawItems.isNullOrBlank()) JSONArray(rawItems) else JSONArray()
                val list = mutableListOf<JSONObject>()

                var existingPoster = poster

                for (i in 0 until array.length()) {
                    val itemObj = array.getJSONObject(i)
                    val sId = itemObj.optLong("streamId", 0L)
                    val itemUrl = itemObj.optString("url", "")

                    val isMatch = (effectiveStreamId > 0 && sId == effectiveStreamId) || (itemUrl.isNotBlank() && itemUrl == url)
                    if (isMatch) {
                        if (existingPoster.isBlank()) {
                            existingPoster = itemObj.optString("poster", "")
                        }
                    } else {
                        list.add(itemObj)
                    }
                }

                val isFinished = durationMs > 0 && (positionMs.toFloat() / durationMs.toFloat()) >= 0.95f
                val isJustStarted = positionMs < 2000

                if (!isFinished && !isJustStarted) {
                    val itemObj = JSONObject().apply {
                        put("streamId", effectiveStreamId)
                        put("title", title)
                        put("poster", existingPoster)
                        put("url", url)
                        put("positionMs", positionMs)
                        put("durationMs", durationMs)
                        put("isSeries", isSeries)
                        put("lastUpdated", System.currentTimeMillis())
                    }
                    // Insert newest at top
                    list.add(0, itemObj)
                }

                // Keep up to 50 recent items
                val finalArray = JSONArray()
                list.take(50).forEach { finalArray.put(it) }

                prefs[itemsKey] = finalArray.toString()
            }
        }
    }

    private fun extractIdFromUrl(url: String): Long {
        return try {
            val digits = Regex("""\d+""").findAll(url).map { it.value.toLong() }.toList()
            digits.lastOrNull() ?: (url.hashCode().toLong().let { if (it < 0) -it else it })
        } catch (_: Exception) {
            url.hashCode().toLong().let { if (it < 0) -it else it }
        }
    }


    suspend fun removeProgress(url: String, streamId: Long = 0) {
        context.progressDataStore.edit { prefs ->
            val json = prefs[progressKey]
            if (!json.isNullOrBlank()) {
                val obj = JSONObject(json)
                obj.remove(url)
                prefs[progressKey] = obj.toString()
            }

            if (streamId > 0) {
                val rawItems = prefs[itemsKey]
                if (!rawItems.isNullOrBlank()) {
                    val array = JSONArray(rawItems)
                    val newArray = JSONArray()
                    for (i in 0 until array.length()) {
                        val itemObj = array.getJSONObject(i)
                        if (itemObj.optLong("streamId") != streamId) {
                            newArray.put(itemObj)
                        }
                    }
                    prefs[itemsKey] = newArray.toString()
                }
            }
        }
    }
}

