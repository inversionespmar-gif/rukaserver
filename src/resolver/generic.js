const BROWSER_HEADERS = {
  "User-Agent":
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
  "Accept": "*/*",
};

function absolutize(found, base) {
  try { return new URL(found, base).href; } catch { return found; }
}

export async function genericResolve(url, { fetchImpl = fetch } = {}) {
  const res = await fetchImpl(url, { headers: BROWSER_HEADERS, redirect: "follow" });
  if (!res.ok) return null;
  const html = await res.text();
  const base = res.url || url;

  const patterns = [
    /(?:src|file|source|hls|url)\s*[:=]\s*["']([^"']+\.(?:m3u8|mp4)(?:\?[^"']*)?)["']/i,
    /["']([^"']+\.(?:m3u8|mp4)(?:\?[^"']*)?)["']/i,
  ];
  for (const re of patterns) {
    const m = html.match(re);
    if (m) {
      const found = absolutize(m[1], base);
      return { url: found, type: found.includes(".m3u8") ? "m3u8" : "mp4" };
    }
  }
  return null;
}
