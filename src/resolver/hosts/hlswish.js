import { genericResolve } from "../generic.js";
const RE = /["']?stream["']?\s*:\s*["']([^"']+\.m3u8)["']/i;
export async function resolveHlswish(url, { fetchImpl = fetch } = {}) {
  const res = await fetchImpl(url, { headers: { "User-Agent": "Mozilla/5.0" }, redirect: "follow" });
  if (!res.ok) return null;
  const html = await res.text();
  const m = html.match(RE);
  if (m) return { url: m[1], type: "m3u8" };
  return genericResolve(url, { fetchImpl });
}
