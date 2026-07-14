export function resolveUrl(maybeRelative, base) {
  if (!maybeRelative) return maybeRelative;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(maybeRelative)) return maybeRelative;
  try { return new URL(maybeRelative, base).href; } catch { return maybeRelative; }
}

function rewriteUriAttr(line, baseUrl, proxyBase) {
  return line.replace(/(URI=["'])([^"']+)(["'])/gi, (m, pre, uri, post) => {
    if (uri.startsWith("/proxy/")) return m; // already proxied
    const abs = resolveUrl(uri, baseUrl);
    return `${pre}${proxyBase}${encodeURIComponent(abs)}${post}`;
  });
}

export function rewriteM3u8(manifest, baseUrl, proxyBase) {
  return manifest
    .split(/\r?\n/)
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed === "") return line;
      if (trimmed.startsWith("#")) {
        if (/URI=/.test(trimmed)) return rewriteUriAttr(line, baseUrl, proxyBase);
        return line;
      }
      const abs = resolveUrl(trimmed, baseUrl);
      return `${proxyBase}${encodeURIComponent(abs)}`;
    })
    .join("\n");
}
