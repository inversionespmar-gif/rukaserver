package com.rukatv.iptv.data.repository

import com.rukatv.iptv.data.local.FavoritesStore
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val store: FavoritesStore) {
    val favorites: Flow<Set<String>> = store.favorites
    suspend fun toggle(id: String) = store.toggle(id)
    suspend fun add(id: String) = store.add(id)
    fun isFavorite(set: Set<String>, id: String) = set.contains(id)
}
