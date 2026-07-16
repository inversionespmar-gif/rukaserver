// Resolves an embed URL to a real stream by loading it in a headless
// Chromium and capturing the first .m3u8 / .mp4 request (or <video> src).
// This is required for hosts that render the player via JavaScript or sit
// behind Cloudflare (vimeos.net, hlswish.com, voe.sx, ...).
//
// IMPORTANT: the browser is launched per resolve and CLOSED immediately
// afterwards. Keeping a persistent Chromium resident is not viable on small
// instances (e.g. Render free, 512 MB) because it starves the rest of the
// process and destabilizes every other request. A simple lock serializes
// resolves so only one browser is alive at a time.
//
// Each resolved candidate is VERIFIED server-side (2xx + media content-type,
// with the Referer captured from the page) before being returned, so blocked
// sources (e.g. vimeos returning 403 without the player's session cookie) are
// skipped in favour of the next embed in the list.

const MEDIA_RE = /\.(m3u8|mp4|ts)(\?[^"\s]*)?$/i;
const UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

let busy = Promise.resolve();

async function launchBrowser() {
  const pw = await import("playwright");
  return pw.chromium.launch({
    args: [
      "--no-sandbox",
      "--disable-setuid-sandbox",
      "--disable-dev-shm-usage",
      "--disable-gpu",
      "--no-zygote",
    ],
  });
}

async function verifyMedia(url, referer, cookies) {
  try {
    const headers = { "User-Agent": UA, "Accept": "*/*" };
    if (referer) headers["Referer"] = referer;
    const cookie = cookieHeaderFor(url, cookies);
    if (cookie) headers["Cookie"] = cookie;
    const res = await fetch(url, { method: "GET", headers, redirect: "follow" });
    if (!res.ok) {
      try { await res.body?.cancel?.(); } catch {}
      return false;
    }
    const ct = res.headers.get("content-type") || "";
    const okType = /mpegurl|mp4|mp2t|video|octet/i.test(ct) || /\.(m3u8|mp4|ts)(\?|$)/i.test(url);
    try { await res.body?.cancel?.(); } catch {}
    return okType;
  } catch {
    return false;
  }
}

function cookieHeaderFor(url, cookies) {
  if (!cookies || !cookies.length) return "";
  let host = "";
  try { host = new URL(url).hostname; } catch { return ""; }
  const parts = [];
  for (const c of cookies) {
    const domain = (c.domain || "").replace(/^\./, "");
    if (domain && (host === domain || host.endsWith("." + domain))) {
      parts.push(`${c.name}=${c.value}`);
    }
  }
  return parts.join("; ");
}

export async function resolveWithBrowser(embedUrl, { timeoutMs = 25000, waitMs = 6000 } = {}) {
  // Hard cap so a slow/unresponsive embed can't make the player wait forever.
  const hardCap = Math.min(timeoutMs, 25000) + waitMs + 8000;
  const run = Promise.race([
    busy.then(async () => {
    let browser;
    try {
      browser = await launchBrowser();
    } catch (e) {
      console.error("[resolver:browser] launch failed:", e && e.message);
      return null;
    }
    let context;
    const candidates = [];
    try {
      context = await browser.newContext({ userAgent: UA });
      const page = await context.newPage();
      const onRequest = (req) => {
        const u = req.url();
        if (MEDIA_RE.test(u)) {
          candidates.push({ url: u, referer: req.headers()["referer"] || "", type: /\.m3u8/i.test(u) ? "m3u8" : "mp4" });
        }
      };
      const onResponse = (res) => {
        const u = res.url();
        const ct = res.headers()["content-type"] || "";
        if (MEDIA_RE.test(u) || /mpegurl/.test(ct) || /video\/mp4/.test(ct)) {
          const ref = res.request()?.headers()?.["referer"] || "";
          candidates.push({ url: u, referer: ref, type: /\.m3u8/i.test(u) ? "m3u8" : "mp4" });
        }
      };
      page.on("request", onRequest);
      page.on("response", onResponse);
      try {
        await page.goto(embedUrl, { waitUntil: "domcontentloaded", timeout: timeoutMs });
      } catch {
        // navigation may "fail" while the player keeps firing media requests
      }
      await new Promise((r) => setTimeout(r, waitMs));
      try {
        const src = await page.$eval("video", (v) => v.currentSrc || v.src || "").catch(() => "");
        if (src && MEDIA_RE.test(src)) {
          candidates.push({ url: src, referer: embedUrl, type: /\.m3u8/i.test(src) ? "m3u8" : "mp4" });
        }
      } catch {}

      if (!candidates.length) return null;

      // Prefer m3u8, then mp4; verify each with a real fetch.
      const ordered = [
        ...candidates.filter((c) => c.type === "m3u8"),
        ...candidates.filter((c) => c.type === "mp4"),
        ...candidates.filter((c) => c.type !== "m3u8" && c.type !== "mp4"),
      ];
      const cookies = await context.cookies().catch(() => []);
      for (const c of ordered) {
        if (await verifyMedia(c.url, c.referer, cookies)) {
          return { url: c.url, type: c.type, referer: c.referer || "", cookies };
        }
      }
      // Nothing verified: return null so the outer resolver loop falls
      // through to the next embed in the list (e.g. hlswish after a
      // blocked vimeos source) instead of serving an unplayable URL.
      return null;
    } catch (e) {
      console.error("[resolver:browser] resolve failed:", e && e.message);
      return null;
    } finally {
      if (context) await context.close().catch(() => {});
      if (browser) await browser.close().catch(() => {});
    }
  }),
    new Promise((resolve) => setTimeout(() => resolve(null), hardCap)),
  ]);
  busy = run.catch(() => null);
  return run;
}
