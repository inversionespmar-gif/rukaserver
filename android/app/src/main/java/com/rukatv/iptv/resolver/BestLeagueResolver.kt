package com.rukatv.iptv.resolver

import android.util.Base64
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class ResolvedStream(
    val mpdUrl: String,
    val channelName: String,
    val keyId: String? = null,
    val key: String? = null
)

class BestLeagueResolver {

    companion object {
        private const val TAG = "BestLeagueResolver"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val TOK_URL = "https://bestleague.top/tok.html"
    }

    private val channelNumbers = mapOf(
        "V2FybmVySEQ=" to 7, "R0VOX1RW" to 7, "VG9kb05vdGljaWFz" to 7,
        "VHlDU3BvcnQ" to 7, "QW1lcmljYTI0" to 7, "QzVO" to 7,
        "TGFfTmFjaW9u" to 7, "Q3JvbmljYVRW" to 7, "Q2FuYWxfOF9UdWN1bWFu" to 7,
        "UGFyYWd1YXlfVFY=" to 7, "UGFyYW1vdW50" to 7, "Q29tZWR5Q2VudHJhbA" to 7,
        "Qm9vbWVyYW5n" to 7, "RHJlYW13b3Jrcw==" to 7, "QW5pbWFsUGxhbmV0" to 7,
        "SGlzdG9yeUhE" to 7, "SUQ=" to 7, "QnJhdm9UVg==" to 7,
        "TkJBX1RW" to 7, "SFRW" to 7, "Rmxvd19NdXNpY18z" to 7,
        "U29ueUhE" to 6, "VHJ1VFY=" to 6, "SEJPX1BPUA==" to 6,
        "Rm94U3BvcnRzMl9VWQ==" to 7, "Rm94U3BvcnRzM19VWQ==" to 7,
        "RVNQTjQ=" to 7, "RVNQTl9VWQ==" to 7, "RVNQTjJfVVk=" to 7,
        "Q2FuYWxfNV9Sb3Nhcmlv" to 4, "Q2FuYWxfOF9DQkE" to 6,
        "QTNfU2VyaWVz" to 7, "VVNBX05ldHdvcms=" to 7,
        "RHNwb3J0c19VWQ==" to 7, "RHNwb3J0czJfVVk=" to 7,
        "RHNwb3J0c19QbHVzX1VZ" to 7
    )

    private val drmKeys = mapOf(
        "V2FybmVySEQ=" to Pair("069bd3f0b6c279467e08549f17bf5bd0", "5afa7e369a6de1093818a85af912a775"),
        "R0VOX1RW" to Pair("49eb924ba998ca7fbbaee30dcef1ecb4", "6e131b04b2e87598fb588ac205673698"),
        "Rm94X1Nwb3J0c19QcmVtaXVuX0hE" to Pair("4c230dbc7f6a4bfa6ad0aa73ff792374", "4186a7c2a15f590a9399886feaec4257"),
        "VG9kb05vdGljaWFz" to Pair("7ceb1cd0622cd7e88fcdc99fe3a55de6", "951637093d41c7388a1ef3f620cfea21"),
        "VHlDU3BvcnQ" to Pair("2b21c8fa9a329cce311a4c4a4aa996a1", "cc23ea1fb32629f9e1f48c8deeae3e5b"),
        "QW1lcmljYTI0" to Pair("3b1b027dd011af20fd9956c16dc084fb", "45f75aacf06593c9b693fe427c67e5b8"),
        "QzVO" to Pair("050df5c6e78c774e78c3e99eef8a1b29", "0e4141d6ab21a36cbc4da777ab3096d4"),
        "TGFfTmFjaW9u" to Pair("f4eade7bbc39b25402acfa301bbad04a", "a74d1df4235a74878327aa8d53ff283c"),
        "Q3JvbmljYVRW" to Pair("745e7abcc90d41ab706b2ac2f4371da3", "50acd9d19d1361cb4a8a13a867bdc352"),
        "Q2FuYWxfOF9UdWN1bWFu" to Pair("7760caa058b51b7cce151c0539fa4a8f", "edd086c1011ed2c54cbe869d0e8d9289"),
        "UGFyYWd1YXlfVFY=" to Pair("68a5bd6c58e6c05bacfd18d3feec31f2", "ae23f8357512df2dfabcb8104b078182"),
        "UGFyYW1vdW50" to Pair("b85b710ecff3e38f31fc8e249b1c1cef", "a1544c193dde6f8858c9358ee69a60a7"),
        "Q29tZWR5Q2VudHJhbA" to Pair("4013f784c5ef4318ad47024e61eb094e", "bad433a547f97c7f65cda5e83b8dd416"),
        "Qm9vbWVyYW5n" to Pair("5792e613fceb699c79cbc0e75fe4cd37", "a672793730476ed23e5c1bce2ff570c6"),
        "RHJlYW13b3Jrcw==" to Pair("7f30c43e47544412221fd64201d92f4b", "f83d09d75a0946b1d71aa48c201b4d8b"),
        "QW5pbWFsUGxhbmV0" to Pair("4146a8ecbb0540dc807c6389ee87e0bc", "0c3cdc1b3e4617c57361265e9fa4c5bd"),
        "SGlzdG9yeUhE" to Pair("e82318e518ba70cea3d7b37bef99e692", "a05fcb634c071a514e3039e1c274b4db"),
        "SUQ=" to Pair("0956caf2e2bd41f49fdcead7cc94fe24", "640c49578073a911938617eb4e652d6c"),
        "QnJhdm9UVg==" to Pair("ad7fbbec39cea4a5a63ac13d94da48d4", "f71087b4dc211db079237c0fb783eb93"),
        "U29ueUhE" to Pair("fd9619f9d7c2d5115a339941279e0b4b", "bf55635e6591f905659fa27ab3ca2812"),
        "VHJ1VFY=" to Pair("7d0cecffe9c29734343cf9983978c1a1", "f86b5ac05f2a2626b6c61bd4e13344d8"),
        "SEJPX1BPUA==" to Pair("f4e1ce5cef7e9a110fe968f8881b21fa", "6bbe2062b150b11496cdd5fbdd9c89d6"),
        "TkJBX1RW" to Pair("d0c38de3c9844e4e9f975dffb3eff8ad", "141ca0fdf6ebadfa7107576b8e09e117"),
        "SFRW" to Pair("daecef5fe32f4ce083c6a0c692755d6a", "d4227f24389a9ba77293214b93eb0d7d"),
        "Rmxvd19NdXNpY18z" to Pair("e078b15ed770ec71f803c0ecc43de033", "7010bccda544f74d1b425c4cebd082d4")
    )

    fun canResolve(url: String): Boolean {
        return url.contains("telelibrefull") ||
            url.contains("bestleague") ||
            url.contains("embed.php") ||
            url.contains("tok.html")
    }

    fun resolve(embedUrl: String): ResolvedStream? {
        try {
            Log.d(TAG, "Resolving: $embedUrl")

            val tokUrl = extractTokUrl(embedUrl)
            if (tokUrl == null) {
                Log.e(TAG, "No se pudo extraer tok URL")
                return null
            }
            Log.d(TAG, "tok URL: $tokUrl")

            val getParam = extractGetParam(tokUrl)
            if (getParam == null) {
                Log.e(TAG, "No get param")
                return null
            }

            val channelName = try {
                String(Base64.decode(getParam, Base64.DEFAULT))
            } catch (e: Exception) {
                Log.e(TAG, "Base64 error", e)
                return null
            }
            Log.d(TAG, "Channel: $channelName")

            val tokHtml = fetchUrl(tokUrl)
            if (tokHtml == null) {
                Log.e(TAG, "No se pudo obtener tok.html")
                return null
            }

            val cdnList = parseCdnTokens(tokHtml)
            if (cdnList.isEmpty()) {
                Log.e(TAG, "No CDN tokens")
                return null
            }
            Log.d(TAG, "CDNs encontrados: ${cdnList.size}")

            val number = channelNumbers[getParam] ?: 3
            val selected = cdnList.random()

            val mpdUrl = "https://${selected.cdn}.cvattv.com.ar/${selected.token}" +
                "/live/c${number}eds/${channelName}" +
                "/SA_Live_dash_enc/${channelName}.mpd"

            Log.d(TAG, "MPD: $mpdUrl")

            val drm = drmKeys[getParam]

            return ResolvedStream(
                mpdUrl = mpdUrl,
                channelName = channelName,
                keyId = drm?.first,
                key = drm?.second
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolviendo", e)
            return null
        }
    }

    private fun extractTokUrl(embedUrl: String): String? {
        val html = fetchUrl(embedUrl) ?: return null

        val iframeRe = Pattern.compile("""<iframe[^>]+src="([^"]+)"""", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val match = iframeRe.matcher(html)
        if (!match.find()) return null

        val raw = match.group(1)?.trim() ?: return null
        val getParam = extractGetParam(raw) ?: extractGetParam(html)

        val domainMatcher = Pattern.compile("""(?:https?://|//)?([a-z0-9.-]*bestleague[a-z0-9.-]*)""", Pattern.CASE_INSENSITIVE).matcher(raw)
        val domain = if (domainMatcher.find()) domainMatcher.group(1) else "bestleague.life"

        if (getParam != null) {
            return "https://$domain/tok.html?get=$getParam"
        }

        var cleanUrl = raw.replace(Regex("""<[^>]*>"""), "").replace("\n", "").replace("\r", "").trim()
        return when {
            cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://") -> cleanUrl
            cleanUrl.startsWith("//") -> "https:$cleanUrl"
            else -> "https://bestleague.life/$cleanUrl"
        }
    }

    private fun extractGetParam(url: String): String? {
        val re = Pattern.compile("""[?&]get=([^&"\s]+)""")
        val match = re.matcher(url)
        return if (match.find()) match.group(1) else null
    }

    private fun parseCdnTokens(html: String): List<CdnToken> {
        val result = mutableListOf<CdnToken>()

        val arrayRe = Pattern.compile("""var mt = \[(.*?)\];""", Pattern.DOTALL)
        val arrayMatch = arrayRe.matcher(html)
        if (!arrayMatch.find()) return result

        val arrayStr = arrayMatch.group(1) ?: return result

        val objRe = Pattern.compile(
            """"cdn"\s*:\s*"([^"]+)".*?"token"\s*:\s*"([^"]+)"""",
            Pattern.DOTALL
        )
        val objMatcher = objRe.matcher(arrayStr)
        while (objMatcher.find()) {
            result.add(CdnToken(objMatcher.group(1)!!, objMatcher.group(2)!!))
        }

        return result
    }

    private fun fetchUrl(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connect()
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "fetch error: ${e.message}")
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    data class CdnToken(val cdn: String, val token: String)
}
