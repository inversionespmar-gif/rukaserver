import { test } from "node:test";
import assert from "node:assert/strict";
import { rewriteM3u8 } from "../src/stream/hls.js";

test("rewriteM3u8 rewrites segment and variant urls through proxy", () => {
  const manifest = `#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=1000
variant.m3u8
#EXTINF:2,
seg1.ts
`;
  const out = rewriteM3u8(manifest, "https://cdn.com/a/", "/proxy/");
  assert.match(out, /\/proxy\//);
  assert.ok(out.includes(encodeURIComponent("https://cdn.com/a/variant.m3u8")));
  assert.ok(out.includes(encodeURIComponent("https://cdn.com/a/seg1.ts")));
});

test("rewriteM3u8 keeps comment lines", () => {
  const out = rewriteM3u8("#EXTM3U\n#EXT-X-ENDLIST\n", "https://c/", "/proxy/");
  assert.ok(out.includes("#EXTM3U"));
  assert.ok(out.includes("#EXT-X-ENDLIST"));
});
