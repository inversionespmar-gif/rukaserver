// Resolver for Upstream / UpstreamVid (upstream.to, upstreamvid.com, etc.)
// These are HLS-based and expose the stream in a jwplayer "file" property.
import { unpackDeanEdwards, scanForUrls } from "../embedExtract.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveUpstream(url, { fetchImpl = fetch } = {}) {
  try {
    const origin = new URL(url).origin;
    const res = await fetchImpl(url, {
      headers: { "User-Agent": UA, "Referer": origin + "/" },
      redirect: "follow",
    });
    if (!res.ok) return null;
    const html = await res.text();

    // file: "https://...m3u8..."
    const fileMatch = html.match(/file\s*:\s*["']([^"']+\.m3u8[^"']*)['"]/i);
    if (fileMatch) return { url: fileMatch[1], type: "m3u8", referer: origin + "/" };

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
    console.error("[resolveUpstream] error:", e?.message);
    return null;
  }
}
