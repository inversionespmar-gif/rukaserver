import { test } from "node:test";
import assert from "node:assert/strict";
import { startApp } from "./helpers.js";
import { createAuth } from "../src/auth.js";
import { createCatalogRepository } from "../src/repositories/catalog.js";
import { createUsersRepository } from "../src/repositories/users.js";
import { createCache } from "../src/cache.js";
import { createDefaultResolver } from "../src/resolver/index.js";

const fakeSupabase = {
  from(table) {
    const data = {
      users: { data: { username: "u", password: "p", status: true, exp_date: Math.floor(Date.now()/1000)+99999, max_connections: 1, is_trial: false }, error: null },
      tv_channels: { data: [{ id: 1, canal: "C1", pais: "PY", m3u8: "https://live.com/c.m3u8", logo: "" }], error: null },
      movie_links: { data: [{ id: 1, tmdb_id: "9", title: "M", player_url: '["https://h/embed-x"]', poster_path: "", overview: "", vote_average: 0, release_date: "", genre_ids: "[]", popularity: 0 }], error: null },
      series_metadata: { data: [], error: null },
      series_episodes: { data: [], error: null },
    }[table];
    const chain = { eq: () => chain, order: async () => data, maybeSingle: async () => data };
    return { select: () => chain };
  },
};

const savedFetch = globalThis.fetch;
const fakeFetch = async (u) => {
  if (String(u).includes("embed-x")) {
    return { ok: true, url: u, text: async () => `<source src="https://cdn.com/a.m3u8">` };
  }
  return {
    ok: true, url: u,
    headers: { get: (h) => (String(h).toLowerCase().includes("content-type") ? "application/vnd.apple.mpegurl" : null) },
    text: async () => `#EXTM3U\nseg1.ts\n`,
  };
};

test("GET /movie/u/p/1.mp4 resolves embed and proxies m3u8", async () => {
  globalThis.fetch = fakeFetch;
  const resolver = createDefaultResolver({ cache: createCache({ ttlMs: 1000 }), fetchImpl: fakeFetch });
  const { app, close } = await startApp({
    auth: createAuth(createUsersRepository(fakeSupabase)),
    catalog: createCatalogRepository(fakeSupabase),
    config: { baseUrl: "http://localhost" },
    resolver,
  });
  try {
    const res = await savedFetch(`${app.origin}/movie/u/p/1.mp4`);
    assert.equal(res.status, 200);
    const body = await res.text();
    assert.match(body, /seg1\.ts/);
  } finally { globalThis.fetch = savedFetch; await close(); }
});
