package com.rukatv.iptv

data class PlayItem(
    val url: String,
    val title: String,
    val streamId: Long = 0L,
    val poster: String = ""
)

