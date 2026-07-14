import { genericResolve } from "../generic.js";
const RE = /(?:file|video)\s*:\s*["']([^"']+\.(?:m3u8|mp4))["']/i;
export async function resolveVoe(url, { fetchImpl = fetch } = {}) {
  const res = await fetchImpl(url, { headers: { "User-Agent": "Mozilla/5.0" }, redirect: "follow" });
  if (!res.ok) return null;
  const html = await res.text();
  const m = html.match(RE);
  if (m) return { url: m[1], type: m[1].includes(".m3u8") ? "m3u8" : "mp4" };
  return genericResolve(url, { fetchImpl });
}
