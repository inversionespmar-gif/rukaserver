function resolveUrl(maybeRelative, base) {
  if (!maybeRelative) return maybeRelative;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(maybeRelative)) return maybeRelative;
  try { return new URL(maybeRelative, base).href; } catch { return maybeRelative; }
}

function buildProxy(proxyBase, abs, token) {
  const enc = encodeURIComponent(abs);
  let p = `${proxyBase}${enc}`;
  if (token) p += `?t=${encodeURIComponent(token)}`;
  return p;
}

export function rewriteMpd(mpdText, mpdUrl, proxyBase, token = "") {
  const base = mpdUrl.replace(/\/[^/]*$/, "/");
  return mpdText.replace(
    /(<SegmentTemplate[^>]*\s)(initialization|media)=["']([^"']+)["']/gi,
    (m, prefix, attr, val) => {
      if (val.startsWith("/proxy/")) return m;
      const abs = resolveUrl(val, base);
      return `${prefix}${attr}="${buildProxy(proxyBase, abs, token)}"`;
    }
  ).replace(
    /<BaseURL>([^<]+)<\/BaseURL>/gi,
    (m, val) => {
      const abs = resolveUrl(val.trim(), base);
      return `<BaseURL>${buildProxy(proxyBase, abs, token)}</BaseURL>`;
    }
  );
}
