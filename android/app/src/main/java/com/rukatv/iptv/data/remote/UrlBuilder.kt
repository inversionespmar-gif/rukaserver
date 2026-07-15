package com.rukatv.iptv.data.remote

object UrlBuilder {
    private fun normalizeScheme(h: String): String {
        val t = h.trim().trimEnd('/')
        return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
    }

    fun apiBase(host: String): String {
        val h = normalizeScheme(host)
        return if (h.endsWith("/player_api.php")) h else "$h/player_api.php"
    }

    fun liveStream(base: String, user: String, pass: String, id: Long): String {
        val b = normalizeScheme(base).trim().trimEnd('/')
        return "$b/live/${enc(user)}/${enc(pass)}/$id.m3u8"
    }

    fun movieStream(base: String, user: String, pass: String, id: Long): String {
        val b = normalizeScheme(base).trim().trimEnd('/')
        return "$b/movie/${enc(user)}/${enc(pass)}/$id.mp4"
    }

    fun seriesStream(base: String, user: String, pass: String, episodeId: Long): String {
        val b = normalizeScheme(base).trim().trimEnd('/')
        return "$b/series/${enc(user)}/${enc(pass)}/$episodeId.m3u8"
    }

    private fun enc(s: String) = s.replace("/", "%2F")
}
