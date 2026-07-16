// Lightweight embed extractor ported from video-embed-link-converter.
// Extracts direct .m3u8 / .mp4 stream URLs from an embed page's HTML using
// regex scanning, Dean-Edwards packed-JS unpacking, base64 decoding and
// unicode unescaping. No browser required. The vimeos player builds the
// stream URL client-side, so for vimeos this finds the URL but the stream
// still needs the session cookie a real browser would capture (the Chromium
// resolver is kept as the final fallback for that).

function makeAbsolute(url, baseUrl) {
  try {
    return new URL(url, baseUrl).href;
  } catch {
    return url;
  }
}

// Unpacker for Dean Edwards Packed JS
export function unpackDeanEdwards(packed) {
  const pattern = /eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[rd]\s*\)\s*\{(?:[^{}]*|\{[^{}]*\})*\}\s*\(\s*(['"`].*?['"`]|[^,]+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*('[^']*'|"[^"]*"|`[^`]*`|.+?)\s*\.split\s*\(\s*['"`]\|['"`]\s*\)/i;
  const match = packed.match(pattern);
  if (!match) return "";

  try {
    let p = match[1];
    if ((p.startsWith("'") && p.endsWith("'")) || (p.startsWith('"') && p.endsWith('"')) || (p.startsWith("`") && p.endsWith("`"))) {
      p = p.slice(1, -1);
    }
    p = p.replace(/\\'/g, "'").replace(/\\"/g, '"');

    const a = parseInt(match[2], 10);
    const c = parseInt(match[3], 10);
    const kStr = match[4];

    let k = [];
    if ((kStr.startsWith("'") && kStr.endsWith("'")) || (kStr.startsWith('"') && kStr.endsWith('"')) || (kStr.startsWith("`") && kStr.endsWith("`"))) {
      k = kStr.slice(1, -1).split("|");
    } else {
      k = kStr.split("|");
    }

    const e = (cc) => {
      return (cc < a ? "" : e(Math.floor(cc / a))) + (cc % a > 35 ? String.fromCharCode(cc % a + 29) : (cc % a).toString(36));
    };

    const d = {};
    for (let i = 0; i < c; i++) {
      d[e(i)] = k[i] || e(i);
    }

    return p.replace(/\b\w+\b/g, (token) => (d[token] !== undefined ? d[token] : token));
  } catch (err) {
    console.error("[embedExtract] unpack error:", err && err.message);
    return "";
  }
}

// Extract base64 strings that look like URLs
export function extractBase64Urls(text) {
  const urls = [];
  const base64Regex = /['"`]([A-Za-z0-9+/]{16,}=*)['"`]/g;
  let match;
  while ((match = base64Regex.exec(text)) !== null) {
    try {
      const decoded = Buffer.from(match[1], "base64").toString("utf-8");
      if (/^https?:\/\/[^\s"'`<>]+/i.test(decoded)) urls.push(decoded);
    } catch {
      // not valid base64/url
    }
  }
  return urls;
}

// Scan a block of text for absolute/relative .m3u8 and .mp4 stream URLs.
export function scanForUrls(text, baseUrl) {
  const found = [];
  const m3u8Regex = /(https?:\/\/[^\s"'`<>]+?\.(?:m3u8)(?:\?[^\s"'`<>]*)?)/gi;
  const mp4Regex = /(https?:\/\/[^\s"'`<>]+?\.(?:mp4)(?:\?[^\s"'`<>]*)?)/gi;
  const relativeRegex = /["'](\/\/[^\s"'`<>]+?\.(?:m3u8|mp4)(?:\?[^\s"'`<>]*)?)["']/gi;
  const localPathRegex = /["'](\/[^\s"'`<>]+\.(?:m3u8|mp4)(?:\?[^\s"'`<>]*)?)["']/gi;

  let m;
  while ((m = m3u8Regex.exec(text)) !== null) found.push(makeAbsolute(m[1].replace(/\\/g, ""), baseUrl));
  while ((m = mp4Regex.exec(text)) !== null) found.push(makeAbsolute(m[1].replace(/\\/g, ""), baseUrl));
  while ((m = relativeRegex.exec(text)) !== null) found.push(makeAbsolute(new URL(baseUrl).protocol + m[1], baseUrl));
  while ((m = localPathRegex.exec(text)) !== null) found.push(makeAbsolute(m[1], baseUrl));

  return [...new Set(found)];
}

// Extract all candidate stream URLs from an embed page HTML.
export function extractStreamUrls(html, baseUrl) {
  const all = [];

  // 1. Standard scan on raw HTML
  all.push(...scanForUrls(html, baseUrl));

  // 2. Unicode-unescaped scan (e.g. \u002F, \u003A)
  const cleaned = html.replace(/\\u([0-9a-fA-F]{4})/g, (_, g) => String.fromCharCode(parseInt(g, 16)));
  if (cleaned !== html) all.push(...scanForUrls(cleaned, baseUrl));

  // 3. Base64-embedded URLs
  all.push(...extractBase64Urls(html));

  // 4. Dean-Edwards packed JS blocks
  const evalBlocks = html.match(/eval\s*\(\s*function\s*\(.*?\.split\s*\(\s*['"`]\|['"`]\s*\)\s*\)\s*\)/gi);
  if (evalBlocks) {
    for (const block of evalBlocks) {
      const unpacked = unpackDeanEdwards(block);
      if (unpacked) all.push(...scanForUrls(unpacked, baseUrl));
    }
  }

  const unique = [...new Set(all)].filter((u) => /\.(m3u8|mp4)(\?|$)/i.test(u));
  const m3u8 = unique.filter((u) => /\.m3u8/i.test(u));
  const mp4 = unique.filter((u) => /\.mp4/i.test(u));
  return { m3u8, mp4, all: unique };
}
