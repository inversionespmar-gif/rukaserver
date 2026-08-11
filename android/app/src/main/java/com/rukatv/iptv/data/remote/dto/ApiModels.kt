package com.rukatv.iptv.data.remote.dto

import com.squareup.moshi.Json

data class AuthResponse(
    val user_info: UserInfo? = null,
    val server_info: ServerInfo? = null
)

data class UserInfo(
    val auth: Int = 0,
    val username: String? = null,
    val message: String? = null
)

data class ServerInfo(
    val url: String? = null,
    val server_protocol: String? = null
)

data class LiveCategory(
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "category_name") val categoryName: String = ""
)

data class SeriesCategory(
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "category_name") val categoryName: String = ""
)

data class LiveStream(
    @Json(name = "stream_id") val streamId: Long = 0,
    val name: String = "",
    @Json(name = "stream_icon") val streamIcon: String = "",
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "stream_url") val streamUrl: String = ""
)

data class CastMember(
    val name: String,
    val role: String = "",
    val photoUrl: String = ""
)

data class VodStream(
    @Json(name = "stream_id") val streamId: Long = 0,
    val name: String = "",
    val poster: String = "",
    val plot: String = "",
    @Json(name = "release_date") val releaseDate: String = "",
    val rating: String = "",
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    val duration: String = "2h 14m",
    val quality: String = "4K",
    val isHd: Boolean = true,
    val backdrop: String = ""
) {
    val year: String
        get() = if (releaseDate.length >= 4) releaseDate.take(4) else "2025"

    val displayRating: String
        get() = if (rating.isNotBlank() && rating != "0" && rating != "0.0") rating else "8.2"

    val isNewRelease: Boolean
        get() = releaseDate.startsWith("2026") || releaseDate.startsWith("2025")
}

data class SeriesItem(
    @Json(name = "series_id") val seriesId: Long = 0,
    val name: String = "",
    val cover: String = "",
    val poster: String = "",
    val plot: String = "",
    @Json(name = "release_date") val releaseDate: String = "",
    val rating: String = "",
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "category_name") val categoryName: String = "",
    val num: Int = 0,
    val seasonsCount: Int = 1,
    val quality: String = "4K"
) {
    val year: String
        get() = if (releaseDate.length >= 4) releaseDate.take(4) else "2025"

    val displayRating: String
        get() = if (rating.isNotBlank() && rating != "0" && rating != "0.0") rating else "8.5"
}

data class SeriesInfo(
    val seasons: List<Season> = emptyList(),
    val info: SeriesMeta = SeriesMeta(),
    val episodes: Map<String, List<Episode>> = emptyMap()
)

data class Season(
    @Json(name = "season_number") val seasonNumber: Int = 1,
    val name: String = "",
    val cover: String = ""
)

data class SeriesMeta(
    val name: String = "",
    val plot: String = "",
    @Json(name = "poster_path") val posterPath: String = "",
    @Json(name = "backdrop_path") val backdropPath: String = "",
    val rating: String = ""
)

data class Episode(
    val id: Long = 0,
    @Json(name = "episode_num") val episodeNum: Int = 0,
    val title: String = "",
    val season: Int = 1,
    @Json(name = "stream_id") val streamId: Long = 0
)

data class FavoriteItem(val id: String, val name: String, val image: String, val key: String)

data class ContinueWatchingItem(
    val streamId: Long,
    val title: String,
    val poster: String,
    val positionMs: Long,
    val durationMs: Long,
    val isSeries: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}

