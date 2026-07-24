// Resolver for ok.ru (odnoklassniki) video embeds.
// ok.ru embeds contain a JSON data attribute with video qualities.
// We pick the best HLS (m3u8) stream first, then fall back to mp4.
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveOkru(url, { fetchImpl = fetch } = {}) {
  try {
    // Normalise both ok.ru/video/ and ok.ru/videoembed/ URLs
    const embedUrl = url.replace(/ok\.ru\/video\//, "ok.ru/videoembed/");
    const res = await fetchImpl(embedUrl, {
      headers: { "User-Agent": UA, "Referer": "https://ok.ru/" },
      redirect: "follow",
    });
    if (!res.ok) return null;
    const html = await res.text();

    // The player metadata is in a data-options JSON attribute on the div.
    const m = html.match(/data-options="([^"]+)"/);
    if (!m) return null;
    const json = m[1].replace(/&amp;/g, "&").replace(/&quot;/g, '"').replace(/&#(\d+);/g, (_, n) => String.fromCharCode(n));
    let data;
    try { data = JSON.parse(json); } catch { return null; }

    const flashvars = data?.flashvars || data?.videoSources || data;
    // Try HLS first
    const hlsUrl = flashvars?.hls || flashvars?.hlsManifestUrl;
    if (hlsUrl) return { url: hlsUrl, type: "m3u8", referer: "https://ok.ru/" };

    // Try videos array (quality list)
    const videos = flashvars?.videos;
    if (Array.isArray(videos) && videos.length) {
      const mp4 = videos.find(v => v.name === "full") || videos[videos.length - 1];
      if (mp4?.url) return { url: mp4.url, type: "mp4", referer: "https://ok.ru/" };
    }
    return null;
  } catch (e) {
    console.error("[resolveOkru] error:", e?.message);
    return null;
  }
}
