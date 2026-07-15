export function resolveUrl(maybeRelative, base) {
  if (!maybeRelative) return maybeRelative;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(maybeRelative)) return maybeRelative;
  try { return new URL(maybeRelative, base).href; } catch { return maybeRelative; }
}

// Build a proxied URL. referer/cookie are embedded in the PATH (after a "|"
// separator) rather than as query params, so the target URL's own query
// string (e.g. signed CDN tokens like x-signature) is never mutated.
// `|` is safe as a separator because encodeURIComponent encodes it (%7C),
// so it can never appear inside the encoded target/referer/cookie parts.
function buildProxy(proxyBase, abs, referer, cookieStr) {
  const enc = encodeURIComponent(abs);
  let p = `${proxyBase}${enc}`;
  if (referer || cookieStr) {
    p += `|${referer ? encodeURIComponent(referer) : ""}|${cookieStr || ""}`;
  }
  return p;
}

function rewriteUriAttr(line, baseUrl, proxyBase, referer, cookieStr) {
  return line.replace(/(URI=["'])([^"']+)(["'])/gi, (m, pre, uri, post) => {
    if (uri.startsWith("/proxy/")) return m; // already proxied
    const abs = resolveUrl(uri, baseUrl);
    return `${pre}${buildProxy(proxyBase, abs, referer, cookieStr)}${post}`;
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
      return buildProxy(proxyBase, abs, referer, cookieStr);
    })
    .join("\n");
}
