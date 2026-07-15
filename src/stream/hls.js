export function resolveUrl(maybeRelative, base) {
  if (!maybeRelative) return maybeRelative;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(maybeRelative)) return maybeRelative;
  try { return new URL(maybeRelative, base).href; } catch { return maybeRelative; }
}

// Proxied URLs carry a short server-side token (?t=...) instead of embedding
// the referer/cookies in the path. This keeps URLs short and avoids characters
// (like "|") that some IPTV players reject, while still forwarding the
// session referer/cookies needed by protected CDNs.
function buildProxy(proxyBase, abs, token) {
  const enc = encodeURIComponent(abs);
  let p = `${proxyBase}${enc}`;
  if (token) p += `?t=${encodeURIComponent(token)}`;
  return p;
}

function rewriteUriAttr(line, baseUrl, proxyBase, token) {
  return line.replace(/(URI=["'])([^"']+)(["'])/gi, (m, pre, uri, post) => {
    if (uri.startsWith("/proxy/")) return m; // already proxied
    const abs = resolveUrl(uri, baseUrl);
    return `${pre}${buildProxy(proxyBase, abs, token)}${post}`;
  });
}

export function rewriteM3u8(manifest, baseUrl, proxyBase, token = "") {
  return manifest
    .split(/\r?\n/)
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed === "") return line;
      if (trimmed.startsWith("#")) {
        if (/URI=/.test(trimmed)) return rewriteUriAttr(line, baseUrl, proxyBase, token);
        return line;
      }
      const abs = resolveUrl(trimmed, baseUrl);
      return buildProxy(proxyBase, abs, token);
    })
    .join("\n");
}
