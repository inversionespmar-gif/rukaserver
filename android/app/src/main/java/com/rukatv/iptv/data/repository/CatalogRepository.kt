package com.rukatv.iptv.data.repository

import com.rukatv.iptv.data.local.Credentials
import com.rukatv.iptv.data.remote.UrlBuilder
import com.rukatv.iptv.data.remote.XtreamApi

class CatalogRepository(
    private val api: XtreamApi,
    val creds: Credentials
) {
    private val u get() = creds.username
    private val p get() = creds.password

    suspend fun liveCategories() = api.liveCategories(u, p)
    suspend fun liveStreams(categoryId: String? = null) = api.liveStreams(u, p, categoryId = categoryId)
    suspend fun vodStreams() = api.vodStreams(u, p)
    suspend fun seriesCategories() = api.seriesCategories(u, p)
    suspend fun seriesList() = api.seriesList(u, p)
    suspend fun seriesInfo(seriesId: Long) = api.seriesInfo(u, p, seriesId = seriesId)

    fun liveUrl(id: Long) = UrlBuilder.liveStream(creds.host, u, p, id)
    fun movieUrl(id: Long) = UrlBuilder.movieStream(creds.host, u, p, id)
    fun seriesUrl(episodeId: Long) = UrlBuilder.seriesStream(creds.host, u, p, episodeId)
}
