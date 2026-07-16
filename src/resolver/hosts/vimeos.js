import { extractStreamUrls } from "../embedExtract.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveVimeos(url, { fetchImpl = fetch } = {}) {
  try {
    const res = await fetchImpl(url, {
      headers: { "User-Agent": UA, Referer: url, Origin: new URL(url).origin },
      redirect: "follow",
    });
    if (!res.ok) return null;
    const html = await res.text();

    const { m3u8, mp4 } = extractStreamUrls(html, url);
    if (m3u8.length || mp4.length) {
      const chosen = m3u8[0] || mp4[0];
      return {
        url: chosen,
        type: chosen.includes(".m3u8") ? "m3u8" : "mp4",
        referer: new URL(url).origin + "/",
        // vimeos requires the session cookie a browser captures; the Chromium
        // resolver fallback (createDefaultResolver) supplies it when present.
        cookies: [],
        needsBrowserCookie: true,
      };
    }
    return null;
  } catch (err) {
    console.error("[resolveVimeos] error:", err && err.message);
    return null;
  }
}
