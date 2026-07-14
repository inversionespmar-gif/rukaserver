export function resolveUrl(maybeRelative, base) {
  if (!maybeRelative) return maybeRelative;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(maybeRelative)) return maybeRelative;
  try { return new URL(maybeRelative, base).href; } catch { return maybeRelative; }
}

export function rewriteM3u8(manifest, baseUrl, proxyBase) {
  return manifest
    .split(/\r?\n/)
    .map((line) => {
      const trimmed = line.trim();
      if (trimmed === "" || trimmed.startsWith("#")) return line;
      const abs = resolveUrl(trimmed, baseUrl);
      return `${proxyBase}${encodeURIComponent(abs)}`;
    })
    .join("\n");
}
