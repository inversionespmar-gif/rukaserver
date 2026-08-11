package com.rukatv.iptv.data.remote

import com.rukatv.iptv.data.remote.dto.AuthResponse
import com.rukatv.iptv.data.remote.dto.LiveCategory
import com.rukatv.iptv.data.remote.dto.LiveStream
import com.rukatv.iptv.data.remote.dto.SeriesCategory
import com.rukatv.iptv.data.remote.dto.SeriesInfo
import com.rukatv.iptv.data.remote.dto.SeriesItem
import com.rukatv.iptv.data.remote.dto.VodStream
import retrofit2.http.GET
import retrofit2.http.Query

interface XtreamApi {
    @GET("player_api.php")
    suspend fun authenticate(
        @Query("username") username: String,
        @Query("password") password: String
    ): AuthResponse

    @GET("player_api.php")
    suspend fun liveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<LiveCategory>

    @GET("player_api.php")
    suspend fun liveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
        @Query("category_id") categoryId: String? = null
    ): List<LiveStream>

    @GET("player_api.php")
    suspend fun vodStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<VodStream>

    @GET("player_api.php")
    suspend fun seriesCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<SeriesCategory>

    @GET("player_api.php")
    suspend fun seriesList(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): List<SeriesItem>

    @GET("player_api.php")
    suspend fun seriesInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_info",
        @Query("series_id") seriesId: Long
    ): SeriesInfo
}
