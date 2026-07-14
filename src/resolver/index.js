import { genericResolve } from "./generic.js";

export function detectHost(url) {
  try { return new URL(url).hostname.replace(/^www\./, ""); } catch { return ""; }
}

export function createResolver({ fetchImpl = fetch, cache, hosts = {} } = {}) {
  async function resolveOne(url) {
    if (cache) {
      const cached = cache.get(url);
      if (cached) return cached;
    }
    const host = detectHost(url);
    const fn = hosts[host] || (() => genericResolve(url, { fetchImpl }));
    let result = null;
    try {
      const specific = hosts[host]
        ? await hosts[host](url, { fetchImpl })
        : await genericResolve(url, { fetchImpl });
      result = specific;
    } catch {
      result = null;
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
