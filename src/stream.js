import { rewriteM3u8 } from "./stream/hls.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

// A movie/series stream_url (or series player_urls) may be stored either as a
// JSON array of embed URLs or as a single URL string. Normalise both into a
// list so the resolver receives the URLs instead of an empty array (which made
// resolver.resolve() return null and the player show nothing).
function asUrlList(v) {
  if (!v) return [];
  if (Array.isArray(v)) return v.filter(Boolean);
  if (typeof v === "string") {
    try {
      let a = JSON.parse(v);
      if (Array.isArray(a)) {
        while (a.length === 1 && Array.isArray(a[0])) a = a[0];
        if (a.length === 1 && typeof a[0] === "string") {
          try {
            const inner = JSON.parse(a[0]);
            if (Array.isArray(inner)) return inner.filter(Boolean);
          } catch {}
        }
        return a.filter(Boolean);
      }
    } catch {}
    return [v];
  }
  return [];
}

// Short-lived tokens mapping to the referer/cookies captured at resolve time.
// Embedded as ?t=<token> in proxied URLs so we don't bloat the URL with a
// multi-KB cookie JSON (which breaks some players and splits on "|").
const proxyTokens = new Map();
const TOKEN_TTL_MS = 60 * 60 * 1000; // 1 hour

function issueToken({ referer, cookies }) {
  const token = Math.random().toString(36).slice(2) + Date.now().toString(36);
  proxyTokens.set(token, { referer: referer || "", cookies: cookies || [], exp: Date.now() + TOKEN_TTL_MS });
  return token;
}

function consumeToken(token) {
  if (!token) return null;
  const entry = proxyTokens.get(token);
  if (!entry) return null;
  if (entry.exp < Date.now()) { proxyTokens.delete(token); return null; }
  return entry;
}

setInterval(() => {
  const now = Date.now();
  for (const [k, v] of proxyTokens) if (v.exp < now) proxyTokens.delete(k);
}, 10 * 60 * 1000).unref?.();

// Fetch a remote stream using a browser-like UA and a Referer matching the
// target host (many CDNs/embeds require a matching Referer or return 403).
async function fetchProxied(url, extra = {}) {
  let referer = "https://example.com/";
  try { referer = `${new URL(url).origin}/`; } catch {}
  return fetch(url, {
    headers: { "User-Agent": UA, "Accept": "*/*", "Referer": referer, ...extra },
    redirect: "follow",
  });
}

function refererHeader(referer) {
  const headers = {};
  if (referer) headers["Referer"] = referer;
  return headers;
}

function cookieHeaderFor(url, cookies) {
  if (!cookies || !cookies.length) return {};
  let host = "";
  try { host = new URL(url).hostname; } catch { return {}; }
  const parts = [];
  for (const c of cookies) {
    const domain = (c.domain || "").replace(/^\./, "");
    if (domain && (host === domain || host.endsWith("." + domain))) {
      parts.push(`${c.name}=${c.value}`);
    }
  }
  return parts.length ? { "Cookie": parts.join("; ") } : {};
}

export function registerStreamRoutes(app, { auth, catalog, resolver, config }) {
  app.get("/proxy/*", async (req, res) => {
    const raw = req.params[0] || "";
    let target = "";
    let referer = "";
    let cookies = [];
    const token = req.query.t ? decodeURIComponent(req.query.t) : "";
    const entry = consumeToken(token);
    if (entry) { referer = entry.referer || ""; cookies = entry.cookies || []; }
    target = decodeURIComponent(raw);
    if (!/^https?:\/\//.test(target)) return res.status(400).end();
    const extra = { ...refererHeader(referer), ...cookieHeaderFor(target, cookies) };
    try {
      const upstream = await fetchProxied(target, extra);
      const ct = upstream.headers.get("content-type") || "";
      const isM3u8 = ct.includes("mpegurl") || ct.includes("vnd.apple.mpegurl") || /\.m3u8(\?|$)/i.test(target);
      res.status(upstream.status);
      if (isM3u8) {
        const text = await upstream.text();
        const base = new URL(target).href;
        const rewritten = rewriteM3u8(text, base, "/proxy/", token);
        res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
        return res.send(rewritten);
      }
      if (ct) res.setHeader("Content-Type", ct);
      res.setHeader("Cache-Control", "no-cache");
      if (upstream.body && typeof upstream.body.pipe === "function") {
        upstream.body.pipe(res);
      } else {
        const buf = Buffer.from(await upstream.arrayBuffer());
        return res.send(buf);
      }
    } catch {
      return res.status(502).send("proxy_error");
    }
  });

  async function requireAuth(req, res) {
    const r = await auth.authenticate(req.params.username, req.params.password);
    if (r.auth !== 1) { res.status(403).end(); return null; }
    return r.user;
  }

  app.get("/live/:username/:password/:id.m3u8", async (req, res) => {
    const user = await requireAuth(req, res);
    if (!user) return;
    const channels = await catalog.getLiveStreams();
    const channel = channels.find((c) => String(c.stream_id) === String(req.params.id));
    if (!channel || !channel.stream_url) return res.status(404).end();
    try {
      const upstream = await fetchProxied(channel.stream_url);
      if (!upstream.ok) return res.status(upstream.status).send("live_upstream_error");
      const ct = upstream.headers.get("content-type") || "";
      // HLS manifests (.m3u8) need their internal URLs rewritten so the player
      // fetches segments through our /proxy endpoint.
      if (ct.includes("mpegurl") || ct.includes("vnd.apple.mpegurl") || channel.stream_url.endsWith(".m3u8")) {
        const text = await upstream.text();
        const base = new URL(channel.stream_url).href;
        const rewritten = rewriteM3u8(text, base, "/proxy/");
        res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
        return res.send(rewritten);
      }
      // MPEG-TS / raw streams (e.g. http://host/play/xxxx with no extension):
      // stream the bytes through so ExoPlayer can demux them. Do NOT buffer the
      // whole body (live TS never ends), just pipe it to the response.
      if (ct) res.setHeader("Content-Type", ct);
      res.setHeader("Cache-Control", "no-cache");
      res.status(200);
      if (upstream.body && typeof upstream.body.pipe === "function") {
        upstream.body.pipe(res);
      } else {
        const buf = Buffer.from(await upstream.arrayBuffer());
        return res.send(buf);
      }
    } catch { return res.status(502).send("live_error"); }
  });

  async function handleMovie(req, res) {
    const user = await requireAuth(req, res);
    if (!user) return;
    const movies = await catalog.getVodStreams();
    const movie = movies.find((m) => String(m.stream_id) === String(req.params.id));
    if (!movie) return res.status(404).end();
    const urls = asUrlList(movie.stream_url);
    const resolved = resolver ? await resolver.resolve(urls, { timeoutMs: 25000, waitMs: 6000 }) : null;
    if (!resolved) return res.status(502).send("unresolved");
    if (resolved.type === "mp4") {
      const t = issueToken({ referer: resolved.referer, cookies: resolved.cookies });
      return res.redirect(307, "/proxy/" + encodeURIComponent(resolved.url) + "?t=" + encodeURIComponent(t));
    }
    const token = issueToken({ referer: resolved.referer, cookies: resolved.cookies });
    try {
      const hdr = { ...refererHeader(resolved.referer), ...cookieHeaderFor(resolved.url, resolved.cookies) };
      const upstream = await fetchProxied(resolved.url, hdr);
      const text = await upstream.text();
      const rewritten = rewriteM3u8(text, resolved.url, "/proxy/", token);
      res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
      return res.send(rewritten);
    } catch { return res.status(502).send("movie_error"); }
  }
  app.get("/movie/:username/:password/:id.mp4", handleMovie);
  app.get("/movie/:username/:password/:id.m3u8", handleMovie);

  async function getEpisodePlayerUrlsSafe(catalog, id) {
    try {
      const playerUrls = await catalog.getEpisodePlayerUrls(id);
      if (Array.isArray(playerUrls) && playerUrls.length) return playerUrls;
      if (typeof playerUrls === "string" && playerUrls.trim()) return playerUrls;
    } catch {}
    return null;
  }

  app.get("/series/:username/:password/:id.m3u8", async (req, res) => {
    const user = await requireAuth(req, res);
    if (!user) return;
    const playerUrls = await getEpisodePlayerUrlsSafe(catalog, req.params.id);
    if (!playerUrls) return res.status(404).end();
    const urls = asUrlList(playerUrls);
    const resolved = resolver ? await resolver.resolve(urls, { timeoutMs: 30000, waitMs: 9000 }) : null;
    if (!resolved) return res.status(502).send("unresolved");
    if (resolved.type === "mp4") {
      const t = issueToken({ referer: resolved.referer, cookies: resolved.cookies });
      return res.redirect(307, "/proxy/" + encodeURIComponent(resolved.url) + "?t=" + encodeURIComponent(t));
    }
    const token = issueToken({ referer: resolved.referer, cookies: resolved.cookies });
    try {
      const hdr = { ...refererHeader(resolved.referer), ...cookieHeaderFor(resolved.url, resolved.cookies) };
      const upstream = await fetchProxied(resolved.url, hdr);
      const text = await upstream.text();
      const rewritten = rewriteM3u8(text, resolved.url, "/proxy/", token);
      res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
      return res.send(rewritten);
    } catch { return res.status(502).send("movie_error"); }
  });
}
