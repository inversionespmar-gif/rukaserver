import { genericResolve } from "./generic.js";

export function detectHost(url) {
  try { return new URL(url).hostname.replace(/^www\./, ""); } catch { return ""; }
}

export function createResolver({ fetchImpl = fetch, cache, hosts = {}, browserResolve = null } = {}) {
  async function resolveOne(url, opts = {}) {
    if (cache) {
      const cached = cache.get(url);
      if (cached) return cached;
    }
    const host = detectHost(url);
    const hostFn = hosts[host];
    let result = null;
    try {
      result = hostFn
        ? await hostFn(url, { fetchImpl })
        : await genericResolve(url, { fetchImpl });
    } catch {
      result = null;
    }
    if (!result || !result.url || result.needsBrowserCookie) {
      if (browserResolve) {
        try {
          const browserResult = await browserResolve(url, opts);
          if (browserResult && browserResult.url) result = browserResult;
        } catch (e) {
          console.error("[resolver] browserResolve failed:", e && e.message);
        }
      }
    }
    if (result && result.url) {
      if (cache) cache.set(url, result);
      return result;
    }
    return null;
  }

  return {
    async resolve(embedUrls, opts = {}) {
      const list = Array.isArray(embedUrls)
        ? embedUrls
        : (() => { try { return JSON.parse(embedUrls || "[]"); } catch { return []; } })();
      for (const u of list) {
        const r = await resolveOne(u, opts);
        if (r) return r;
      }
      return null;
    },
  };
}

import { resolveVimeos } from "./hosts/vimeos.js";
import { resolveHlswish } from "./hosts/hlswish.js";
import { resolveVoe } from "./hosts/voe.js";
import { resolveGoodstream } from "./hosts/goodstream.js";
import { resolveOkru } from "./hosts/okru.js";
import { resolveDoodstream } from "./hosts/doodstream.js";
import { resolveStreamtape } from "./hosts/streamtape.js";
import { resolveFilemoon } from "./hosts/filemoon.js";
import { resolveMixdrop } from "./hosts/mixdrop.js";
import { resolveStreamwish } from "./hosts/streamwish.js";
import { resolveUpstream } from "./hosts/upstream.js";

export function createDefaultResolver({ fetchImpl = fetch, cache, hosts = {}, browserResolve = null } = {}) {
  const merged = {
    // ── Original hosts ────────────────────────────────────────────────────────
    "vimeos.net": resolveVimeos,
    "hlswish.com": resolveHlswish,
    "voe.sx": resolveVoe,
    "goodstream.one": resolveGoodstream,

    // ── ok.ru / Odnoklassniki ─────────────────────────────────────────────────
    "ok.ru": resolveOkru,
    "odnoklassniki.ru": resolveOkru,

    // ── DoodStream & mirrors ──────────────────────────────────────────────────
    "doodstream.com": resolveDoodstream,
    "dood.la": resolveDoodstream,
    "dood.li": resolveDoodstream,
    "dood.ws": resolveDoodstream,
    "dood.sh": resolveDoodstream,
    "dood.pm": resolveDoodstream,
    "dood.to": resolveDoodstream,
    "dood.so": resolveDoodstream,
    "dood.cx": resolveDoodstream,
    "ds2play.com": resolveDoodstream,
    "doods.pro": resolveDoodstream,

    // ── StreamTape & mirrors ──────────────────────────────────────────────────
    "streamtape.com": resolveStreamtape,
    "streamtape.net": resolveStreamtape,
    "streamtape.co": resolveStreamtape,
    "streamtape.cc": resolveStreamtape,
    "streamtape.to": resolveStreamtape,
    "streamtape.xyz": resolveStreamtape,
    "streamtape.link": resolveStreamtape,
    "tapecontent.net": resolveStreamtape,

    // ── Filemoon & mirrors ────────────────────────────────────────────────────
    "filemoon.sx": resolveFilemoon,
    "filemoon.in": resolveFilemoon,
    "filemoon.to": resolveFilemoon,
    "filemoon.nl": resolveFilemoon,
    "moonplayer.to": resolveFilemoon,
    "moviesm4u.xyz": resolveFilemoon,
    "kerapoxy.cc": resolveFilemoon,

    // ── Mixdrop & mirrors ─────────────────────────────────────────────────────
    "mixdrop.ag": resolveMixdrop,
    "mixdrop.co": resolveMixdrop,
    "mixdrop.bz": resolveMixdrop,
    "mixdrop.ch": resolveMixdrop,
    "mixdrop.gl": resolveMixdrop,
    "mixdrop.to": resolveMixdrop,
    "mixdrop.club": resolveMixdrop,
    "mixdrop.vc": resolveMixdrop,
    "mixdrop.me": resolveMixdrop,

    // ── StreamWish / WishEmbed ────────────────────────────────────────────────
    "streamwish.com": resolveStreamwish,
    "streamwish.to": resolveStreamwish,
    "embedwish.com": resolveStreamwish,
    "wishfast.top": resolveStreamwish,
    "awish.net": resolveStreamwish,
    "swdyu.com": resolveStreamwish,
    "strwish.com": resolveStreamwish,
    "uqloads.xyz": resolveStreamwish,

    // ── Upstream ──────────────────────────────────────────────────────────────
    "upstream.to": resolveUpstream,
    "upstreamvid.com": resolveUpstream,
    "upns.net": resolveUpstream,

    ...hosts,
  };
  return createResolver({ fetchImpl, cache, hosts: merged, browserResolve });
}
