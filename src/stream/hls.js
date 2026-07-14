export function resolveUrl(maybeRelative, base) {
  if (!maybeRelative) return maybeRelative;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(maybeRelative)) return maybeRelative;
  try { return new URL(maybeRelative, base).href; } catch { return maybeRelative; }
}

function withRef(uri, referer, cookie) {
  // Never append to URLs that already carry a query string: many CDNs
  // (e.g. TikTok/signed ad creatives) sign the query, and adding params
  // there breaks the signature. Those URLs are self-contained and don't
  // need our referer/cookie anyway.
  if (uri.includes("?")) return uri;
  if (referer) {
    uri = `${uri}?ref=${encodeURIComponent(referer)}`;
  }
  if (cookie) {
    const sep = uri.includes("?") ? "&" : "?";
    uri = `${uri}${sep}cookie=${encodeURIComponent(cookie)}`;
  }
  return uri;
}

function rewriteUriAttr(line, baseUrl, proxyBase, referer, cookie) {
  return line.replace(/(URI=["'])([^"']+)(["'])/gi, (m, pre, uri, post) => {
    if (uri.startsWith("/proxy/")) return m; // already proxied
    const abs = resolveUrl(uri, baseUrl);
    return `${pre}${withRef(`${proxyBase}${encodeURIComponent(abs)}`, referer, cookie)}${post}`;
  });
}

export function rewriteM3u8(manifest, baseUrl, proxyBase, referer = "", cookies = []) {
  const cookieStr = cookies && cookies.length ? encodeURIComponent(JSON.stringify(cookies)) : "";
  return manifest
    .split(/\r?\n/)
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed === "") return line;
      if (trimmed.startsWith("#")) {
        if (/URI=/.test(trimmed)) return rewriteUriAttr(line, baseUrl, proxyBase, referer, cookieStr);
        return line;
      }
      const abs = resolveUrl(trimmed, baseUrl);
      return withRef(`${proxyBase}${encodeURIComponent(abs)}`, referer, cookieStr);
    })
    .join("\n");
}
