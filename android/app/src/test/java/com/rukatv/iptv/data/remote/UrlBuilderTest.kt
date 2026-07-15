package com.rukatv.iptv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlBuilderTest {
    @Test
    fun buildsLiveUrl() {
        assertEquals(
            "https://example.com/live/u/p/5.m3u8",
            UrlBuilder.liveStream("https://example.com/", "u", "p", 5)
        )
    }

    @Test
    fun buildsMovieAndSeriesUrls() {
        assertEquals(
            "https://x.com/movie/u/p/9.mp4",
            UrlBuilder.movieStream("https://x.com", "u", "p", 9)
        )
        assertEquals(
            "https://x.com/series/u/p/12.m3u8",
            UrlBuilder.seriesStream("https://x.com/", "u", "p", 12)
        )
    }

    @Test
    fun apiBaseNormalizesTrailingSlash() {
        assertEquals("https://x.com/player_api.php", UrlBuilder.apiBase("https://x.com/"))
    }
}
