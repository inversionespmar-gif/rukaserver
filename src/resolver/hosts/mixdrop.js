// Resolver for Mixdrop (mixdrop.ag, mixdrop.co, etc.)
// Mixdrop embeds the stream URL in a JS variable "MDCore.wurl" or "MDCore.ref"
// inside an eval block (often Dean-Edwards packed).
import { unpackDeanEdwards } from "../embedExtract.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveMixdrop(url, { fetchImpl = fetch } = {}) {
  try {
    const origin = new URL(url).origin;
    const res = await fetchImpl(url, {
      headers: { "User-Agent": UA, "Referer": origin + "/" },
      redirect: "follow",
    });
    if (!res.ok) return null;
    let html = await res.text();

    // Unpack any packed JS first
    const evalBlocks = html.match(/eval\s*\(\s*function\s*\(.*?\.split\s*\(\s*['"`]\|['"`]\s*\)\s*\)/gi);
    if (evalBlocks) {
      for (const block of evalBlocks) {
        const unpacked = unpackDeanEdwards(block);
        if (unpacked) html = html + "\n" + unpacked;
      }
    }

    // MDCore.wurl = "//cdn.mixdrop.co/..."
    const wurlMatch = html.match(/MDCore\.wurl\s*=\s*["']([^"']+)["']/);
    if (wurlMatch) {
      const raw = wurlMatch[1];
      const streamUrl = raw.startsWith("//") ? "https:" + raw : raw;
      return { url: streamUrl, type: streamUrl.includes(".m3u8") ? "m3u8" : "mp4", referer: origin + "/" };
    }

    // Fallback: any absolute mp4/m3u8 URL
    const anyMp4 = html.match(/https?:\/\/[^\s"'<>]+\.mp4[^\s"'<>]*/i);
    if (anyMp4) return { url: anyMp4[0], type: "mp4", referer: origin + "/" };

    return null;
  } catch (e) {
    console.error("[resolveMixdrop] error:", e?.message);
    return null;
  }
}
