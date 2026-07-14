import { test } from "node:test";
import assert from "node:assert/strict";
import { createCache } from "../src/cache.js";
import { createResolver } from "../src/resolver/index.js";
import { genericResolve } from "../src/resolver/generic.js";
import { resolveVimeos } from "../src/resolver/hosts/vimeos.js";
import { resolveHlswish } from "../src/resolver/hosts/hlswish.js";
import { resolveVoe } from "../src/resolver/hosts/voe.js";
import { resolveGoodstream } from "../src/resolver/hosts/goodstream.js";

test("genericResolve extracts m3u8 from html", async () => {
  const fakeFetch = async () => ({
    ok: true,
    text: async () => `<video><source src="/path/live.m3u8"></video>`,
  });
  const r = await genericResolve("https://host.com/embed-x.html", { fetchImpl: fakeFetch });
  assert.equal(r.type, "m3u8");
  assert.equal(r.url, "https://host.com/path/live.m3u8");
});

test("genericResolve extracts mp4 from json-ish script", async () => {
  const fakeFetch = async () => ({
    ok: true,
    text: async () => `var cfg = { file: "https://cdn.com/v.mp4" };`,
  });
  const r = await genericResolve("https://host.com/e/x", { fetchImpl: fakeFetch });
  assert.equal(r.type, "mp4");
  assert.equal(r.url, "https://cdn.com/v.mp4");
});

test("resolver tries each url and caches success", async () => {
  const cache = createCache({ ttlMs: 1000 });
  const fakeFetch = async (u) => ({
    ok: true,
    text: async () => u.includes("bad") ? `<html></html>` : `<source src="https://cdn.com/a.m3u8">`,
  });
  const resolver = createResolver({ fetchImpl: fakeFetch, cache, hosts: {} });
  const urls = ["https://h/bad", "https://h/good"];
  const r = await resolver.resolve(urls);
  assert.equal(r.url, "https://cdn.com/a.m3u8");
  const r2 = await resolver.resolve(urls);
  assert.equal(r2.url, "https://cdn.com/a.m3u8");
});

test("resolver returns null when nothing resolves", async () => {
  const cache = createCache({ ttlMs: 1000 });
  const fakeFetch = async () => ({ ok: true, text: async () => `<html></html>` });
  const resolver = createResolver({ fetchImpl: fakeFetch, cache, hosts: {} });
  const r = await resolver.resolve(["https://h/x"]);
  assert.equal(r, null);
});

test("vimeos resolver extracts master m3u8", async () => {
  const fakeFetch = async () => ({ ok: true, text: async () =>
    `<script>var player = { sources:[{file:"https://v.m3u8"}] };</script>` });
  const r = await resolveVimeos("https://vimeos.net/embed-x.html", { fetchImpl: fakeFetch });
  assert.equal(r.type, "m3u8");
  assert.equal(r.url, "https://v.m3u8");
});

test("hlswish resolver extracts file", async () => {
  const fakeFetch = async () => ({ ok: true, text: async () =>
    `window.config = {"stream":"https://hlswish.com/s/abc.m3u8"}` });
  const r = await resolveHlswish("https://hlswish.com/e/x", { fetchImpl: fakeFetch });
  assert.equal(r.url, "https://hlswish.com/s/abc.m3u8");
});

test("voe resolver extracts the file field", async () => {
  const fakeFetch = async () => ({ ok: true, text: async () =>
    `<script>var l = {file:"https://voe.sx/dl/abc.mp4"};</script>` });
  const r = await resolveVoe("https://voe.sx/e/x", { fetchImpl: fakeFetch });
  assert.equal(r.url, "https://voe.sx/dl/abc.mp4");
});

test("goodstream resolver extracts embed source", async () => {
  const fakeFetch = async () => ({ ok: true, text: async () =>
    `<iframe src="https://goodstream.one/v/abc.m3u8"></iframe>` });
  const r = await resolveGoodstream("https://goodstream.one/embed-x", { fetchImpl: fakeFetch });
  assert.equal(r.url, "https://goodstream.one/v/abc.m3u8");
});
