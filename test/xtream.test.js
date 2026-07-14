import { test } from "node:test";
import assert from "node:assert/strict";
import { startApp } from "./helpers.js";
import { createAuth } from "../src/auth.js";
import { createCatalogRepository } from "../src/repositories/catalog.js";
import { createUsersRepository } from "../src/repositories/users.js";

function chainable(result) {
  return {
    eq: () => chainable(result),
    order: async () => result,
    maybeSingle: async () => result,
  };
}

const fakeSupabase = {
  from(table) {
    const data = {
      users: { data: { username: "u", password: "p", status: true,
        exp_date: Math.floor(Date.now() / 1000) + 99999, max_connections: 1, is_trial: false }, error: null },
      tv_channels: { data: [{ id: 1, canal: "Canal 1", pais: "PY", m3u8: "https://h/live.m3u8", logo: "https://h/logo.png" }], error: null },
      movie_links: { data: [{ id: 1, tmdb_id: "9", title: "Película", player_url: "[]", poster_path: "/p.jpg", overview: "", vote_average: 5, release_date: "", genre_ids: "[]", popularity: 1 }], error: null },
      series_metadata: { data: [], error: null },
      series_episodes: { data: [], error: null },
    }[table];
    return {
      select: () => chainable(data),
    };
  },
};

const cfg = { baseUrl: "http://localhost:3000", tmdbToken: "", supabaseUrl: "", supabaseKey: "" };

test("player_api auth action returns auth:1", async () => {
  const { app, close } = await startApp({
    auth: createAuth(createUsersRepository(fakeSupabase)),
    catalog: createCatalogRepository(fakeSupabase),
    config: cfg,
  });
  try {
    const res = await fetch(`${app.origin}/player_api.php?username=u&password=p`);
    const body = await res.json();
    assert.equal(body.user_info.auth, 1);
    assert.equal(body.user_info.username, "u");
  } finally { await close(); }
});

test("player_api get_live_streams returns channels", async () => {
  const { app, close } = await startApp({
    auth: createAuth(createUsersRepository(fakeSupabase)),
    catalog: createCatalogRepository(fakeSupabase),
    config: cfg,
  });
  try {
    const res = await fetch(`${app.origin}/player_api.php?username=u&password=p&action=get_live_streams`);
    const body = await res.json();
    assert.equal(body[0].name, "Canal 1");
    assert.equal(body[0].stream_type, "live");
  } finally { await close(); }
});

test("player_api without credentials returns auth:0", async () => {
  const { app, close } = await startApp({
    auth: createAuth(createUsersRepository(fakeSupabase)),
    catalog: createCatalogRepository(fakeSupabase),
    config: cfg,
  });
  try {
    const res = await fetch(`${app.origin}/player_api.php`);
    const body = await res.json();
    assert.equal(body.user_info.auth, 0);
  } finally { await close(); }
});
