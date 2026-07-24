// Resolver for StreamWish / WishEmbed (streamwish.com, embedwish.com, etc.)
// These hosts expose a jwplayer sources array in the page HTML with the m3u8.
import { unpackDeanEdwards, scanForUrls } from "../embedExtract.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveStreamwish(url, { fetchImpl = fetch } = {}) {
  try {
    const origin = new URL(url).origin;
    const res = await fetchImpl(url, {
      headers: {
        "User-Agent": UA,
        "Referer": origin + "/",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      },
      redirect: "follow",
    });
    if (!res.ok) return null;
    const html = await res.text();

    // StreamWish: jwplayer sources array
    const sourcesMatch = html.match(/sources\s*:\s*\[([^\]]+)\]/i);
    if (sourcesMatch) {
      const m3u8 = sourcesMatch[1].match(/file\s*:\s*["']([^"']+\.m3u8[^"']*)['"]/i);
      if (m3u8) return { url: m3u8[1], type: "m3u8", referer: origin + "/" };
      const mp4 = sourcesMatch[1].match(/file\s*:\s*["']([^"']+\.mp4[^"']*)['"]/i);
      if (mp4) return { url: mp4[1], type: "mp4", referer: origin + "/" };
    }

    // Packed JS fallback
    const evalBlocks = html.match(/eval\s*\(\s*function\s*\(.*?\.split\s*\(\s*['"`]\|['"`]\s*\)\s*\)/gi);
    if (evalBlocks) {
      for (const block of evalBlocks) {
        const unpacked = unpackDeanEdwards(block);
        if (unpacked) {
          const urls = scanForUrls(unpacked, url);
          const chosen = urls.find(u => /\.m3u8/i.test(u)) || urls.find(u => /\.mp4/i.test(u));
          if (chosen) return { url: chosen, type: chosen.includes(".m3u8") ? "m3u8" : "mp4", referer: origin + "/" };
        }
      }
    }

    return null;
  } catch (e) {
    console.error("[resolveStreamwish] error:", e?.message);
    return null;
  }
}
