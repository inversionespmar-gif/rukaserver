// Resolver for Filemoon (filemoon.sx, filemoon.in, etc.)
// Filemoon packs the stream URL in a Dean Edwards eval(function(...)) block.
// We reuse the embedExtract unpacker that handles packed JS.
import { unpackDeanEdwards, scanForUrls } from "../embedExtract.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveFilemoon(url, { fetchImpl = fetch } = {}) {
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

    // First: try packed JS blocks (most Filemoon pages use this)
    const evalBlocks = html.match(/eval\s*\(\s*function\s*\(.*?\.split\s*\(\s*['"`]\|['"`]\s*\)\s*\)/gi);
    if (evalBlocks) {
      for (const block of evalBlocks) {
        const unpacked = unpackDeanEdwards(block);
        if (unpacked) {
          const urls = scanForUrls(unpacked, url);
          if (urls.length) {
            const m3u8 = urls.find(u => /\.m3u8/i.test(u));
            const mp4 = urls.find(u => /\.mp4/i.test(u));
            const chosen = m3u8 || mp4;
            if (chosen) return { url: chosen, type: m3u8 ? "m3u8" : "mp4", referer: origin + "/" };
          }
        }
      }
    }

    // Fallback: direct regex scan
    const directM3u8 = html.match(/["']?(https?:\/\/[^"'\s]+\.m3u8[^"'\s]*)["']?/i);
    if (directM3u8) return { url: directM3u8[1], type: "m3u8", referer: origin + "/" };

    return null;
  } catch (e) {
    console.error("[resolveFilemoon] error:", e?.message);
    return null;
  }
}
