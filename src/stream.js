import { rewriteM3u8 } from "./stream/hls.js";
import { parseJsonArray } from "./repositories/catalog.js";

const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

function proxyHeaders() {
  return { "User-Agent": UA, "Accept": "*/*", "Referer": "https://example.com/" };
}

export function registerStreamRoutes(app, { auth, catalog, resolver, config }) {
  app.get("/proxy/*", async (req, res) => {
    const target = decodeURIComponent(req.params[0] || "");
    if (!/^https?:\/\//.test(target)) return res.status(400).end();
    try {
      const upstream = await fetch(target, { headers: proxyHeaders(), redirect: "follow" });
      const ct = upstream.headers.get("content-type") || "";
      res.status(upstream.status);
      if (ct.includes("mpegurl") || ct.includes("vnd.apple.mpegurl")) {
        const text = await upstream.text();
        const base = new URL(target).href;
        const rewritten = rewriteM3u8(text, base, "/proxy/");
        res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
        return res.send(rewritten);
      }
      const cl = upstream.headers.get("content-length");
      if (cl) res.setHeader("content-length", cl);
      const buf = Buffer.from(await upstream.arrayBuffer());
      return res.send(buf);
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
      const upstream = await fetch(channel.stream_url, { headers: proxyHeaders(), redirect: "follow" });
      const text = await upstream.text();
      const rewritten = rewriteM3u8(text, channel.stream_url, "/proxy/");
      res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
      return res.send(rewritten);
    } catch { return res.status(502).send("live_error"); }
  });

  app.get("/movie/:username/:password/:id.mp4", async (req, res) => {
    const user = await requireAuth(req, res);
    if (!user) return;
    const movies = await catalog.getVodStreams();
    const movie = movies.find((m) => String(m.stream_id) === String(req.params.id));
    if (!movie) return res.status(404).end();
    const urls = parseJsonArray(movie.stream_url);
    const resolved = resolver ? await resolver.resolve(urls) : null;
    if (!resolved) return res.status(502).send("unresolved");
    if (resolved.type === "mp4") return res.redirect(302, resolved.url);
    try {
      const upstream = await fetch(resolved.url, { headers: proxyHeaders(), redirect: "follow" });
      const text = await upstream.text();
      const rewritten = rewriteM3u8(text, resolved.url, "/proxy/");
      res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
      return res.send(rewritten);
    } catch { return res.status(502).send("movie_error"); }
  });

  app.get("/series/:username/:password/:id.m3u8", async (req, res) => {
    const user = await requireAuth(req, res);
    if (!user) return;
    const playerUrls = await catalog.getEpisodePlayerUrls(req.params.id);
    if (!playerUrls) return res.status(404).end();
    const urls = parseJsonArray(playerUrls);
    const resolved = resolver ? await resolver.resolve(urls) : null;
    if (!resolved) return res.status(502).send("unresolved");
    if (resolved.type === "mp4") return res.redirect(302, resolved.url);
    try {
      const upstream = await fetch(resolved.url, { headers: proxyHeaders(), redirect: "follow" });
      const text = await upstream.text();
      const rewritten = rewriteM3u8(text, resolved.url, "/proxy/");
      res.setHeader("Content-Type", "application/vnd.apple.mpegurl");
      return res.send(rewritten);
    } catch { return res.status(502).send("series_error"); }
  });
}
