// Resolver for DoodStream (dood.la, doodstream.com, ds2play.com, etc.)
// DoodStream serves an obfuscated token stream URL.
// Flow: fetch the embed page → extract /pass_md5/... path → fetch that path
// with the correct Referer → assemble final URL with ?token=&expiry= params.
const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

// Generates the random suffix DoodStream appends to the base MP4 URL.
function randomStr(length = 10) {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join("");
}

export async function resolveDoodstream(url, { fetchImpl = fetch } = {}) {
  try {
    const origin = new URL(url).origin;
    const headers = {
      "User-Agent": UA,
      "Referer": origin + "/",
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    };

    const res = await fetchImpl(url, { headers, redirect: "follow" });
    if (!res.ok) return null;
    const html = await res.text();

    // Extract the /pass_md5/... path used to get the actual stream token.
    // Multiple regex patterns to handle different DoodStream embed layouts.
    const passMatch =
      html.match(/\/pass_md5\/[^"'\s<>?]+/) ||
      html.match(/pass_md5=['"]([^"']+)['"]/) ||
      html.match(/\$\.get\(['"]([^'"]*pass_md5[^'"]*)['"]/);

    if (!passMatch) return null;
    const passPath = passMatch[1] || passMatch[0];

    // Fetch the token endpoint (must send Referer = original embed page).
    const tokenUrl = passPath.startsWith("http") ? passPath : origin + passPath;
    const tokenRes = await fetchImpl(tokenUrl, {
      headers: { "User-Agent": UA, "Referer": url },
      redirect: "follow",
    });
    if (!tokenRes.ok) return null;
    const tokenBase = (await tokenRes.text()).trim();
    if (!tokenBase || !tokenBase.startsWith("http")) return null;

    // Extract the token query param DoodStream expects.
    const tokenMatch = html.match(/token=([a-zA-Z0-9]+)/);
    const token = tokenMatch ? tokenMatch[1] : randomStr(10);
    const expiry = Math.floor(Date.now() / 1000) + 3600;
    const rand = randomStr(10);

    const streamUrl = `${tokenBase}${rand}?token=${token}&expiry=${expiry}`;
    return { url: streamUrl, type: "mp4", referer: url };
  } catch (e) {
    console.error("[resolveDoodstream] error:", e?.message);
    return null;
  }
}
