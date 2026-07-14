import { genericResolve } from "./generic.js";

export function detectHost(url) {
  try { return new URL(url).hostname.replace(/^www\./, ""); } catch { return ""; }
}

export function createResolver({ fetchImpl = fetch, cache, hosts = {}, browserResolve = null } = {}) {
  async function resolveOne(url) {
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
    if (!result || !result.url) {
      if (browserResolve) {
        try { result = await browserResolve(url); } catch (e) {
          console.error("[resolver] browserResolve failed:", e && e.message);
          result = null;
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
    async resolve(embedUrls) {
      const list = Array.isArray(embedUrls)
        ? embedUrls
        : (() => { try { return JSON.parse(embedUrls || "[]"); } catch { return []; } })();
      for (const u of list) {
        const r = await resolveOne(u);
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

export function createDefaultResolver({ fetchImpl = fetch, cache, hosts = {}, browserResolve = null } = {}) {
  const merged = {
    "vimeos.net": resolveVimeos,
    "hlswish.com": resolveHlswish,
    "voe.sx": resolveVoe,
    "goodstream.one": resolveGoodstream,
    ...hosts,
  };
  return createResolver({ fetchImpl, cache, hosts: merged, browserResolve });
}
