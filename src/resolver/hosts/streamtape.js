// Resolver for StreamTape (streamtape.com, streamtape.net, etc.)
// StreamTape obfuscates the download link in two JS variables that must be
// concatenated. The first part is in a comment-like span, the second in a
// JS string assigned right after.
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

export async function resolveStreamtape(url, { fetchImpl = fetch } = {}) {
  try {
    const origin = new URL(url).origin;
    const res = await fetchImpl(url, {
      headers: { "User-Agent": UA, "Referer": origin + "/" },
      redirect: "follow",
    });
    if (!res.ok) return null;
    const html = await res.text();

    // StreamTape pattern: two adjacent JS vars that form the URL when concatenated.
    // var _0x... = "//streamtape.com/get_video?id=xxx&expires=yyy&ip=zzz&token=aaa";
    // document.getElementById('ideoooolink').innerHTML = (''+(_0x...)).substring(0,...)+'bbbbb'
    const part1Match = html.match(/id=['"]ideoooolink['"][^>]*>.*?['"]([^'"]+)['"]/s);
    // Newer pattern
    const newPattern = html.match(/get_video\?[^'"<\s]+/);
    if (newPattern) {
      const rawUrl = newPattern[0].replace(/&amp;/g, "&");
      const streamUrl = rawUrl.startsWith("http") ? rawUrl : "https://streamtape.com/" + rawUrl;
      return { url: streamUrl, type: "mp4", referer: url };
    }

    // Fallback: two-variable concatenation pattern
    const m1 = html.match(/document\.getElementById\(['"]\w+['"]\)\.innerHTML\s*=\s*\([^+]+\+\s*['"]([^'"]+)['"]\)/);
    const m2 = html.match(/var\s+\w+\s*=\s*['"]([^'"]+\/get_video[^'"]+)['"]/);
    if (m2) {
      const base = m2[1].startsWith("//") ? "https:" + m2[1] : m2[1];
      const suffix = m1 ? m1[1] : "";
      const streamUrl = (base + suffix).replace(/&amp;/g, "&");
      return { url: streamUrl, type: "mp4", referer: url };
    }
    return null;
  } catch (e) {
    console.error("[resolveStreamtape] error:", e?.message);
    return null;
  }
}
