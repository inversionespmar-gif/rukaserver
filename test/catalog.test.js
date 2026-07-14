import { test } from "node:test";
import assert from "node:assert/strict";
import { createCatalogRepository } from "../src/repositories/catalog.js";
import { tmdbImage } from "../src/tmdb.js";

function makeFakeSupabase(tables) {
  const builder = (rows) => {
    let result = rows;
    const b = {
      error: null,
      get data() { return result; },
      select() { return b; },
      eq(field, value) { result = result.filter((r) => r[field] === value); return b; },
      order() { return b; },
      maybeSingle() { return { data: result && result.length ? result[0] : null, error: null }; },
    };
    return b;
  };
  return { from(table) { return builder(tables[table] || []); } };
}

const tables = {
  tv_channels: [
    { id: 1, canal: "C1", pais: "PY", m3u8: "https://h/l.m3u8", logo: "https://h/logo.png" },
    { id: 2, canal: "C2", pais: "AR", m3u8: "https://h/l2.m3u8", logo: "" },
  ],
  movie_links: [{ id: 5, tmdb_id: "1", title: "Movie A", poster_path: "/p.jpg", backdrop_path: "/b.jpg", overview: "plot", vote_average: 7.5, release_date: "2024-01-01", genre_ids: "[]", popularity: 9 }],
  series_metadata: [{ id: 10, tmdb_id: "99", title: "Serie X", poster_path: "/s.jpg", backdrop_path: "/sb.jpg", overview: "so", vote_average: 8, first_air_date: "2023-01-01", genre_ids: "[]", popularity: 1 }],
  series_episodes: [
    { id: 100, tmdb_id: "99", season_number: 1, episode_number: 1, title: "E1", player_urls: "[]" },
    { id: 101, tmdb_id: "99", season_number: 1, episode_number: 2, title: "E2", player_urls: "[]" },
  ],
};

test("tmdbImage builds poster and backdrop urls", () => {
  assert.equal(tmdbImage("/a.jpg", "w500"), "https://image.tmdb.org/t/p/w500/a.jpg");
  assert.equal(tmdbImage("/a.jpg", "w1280"), "https://image.tmdb.org/t/p/w1280/a.jpg");
  assert.equal(tmdbImage("", "w500"), "");
});

test("getVodStreams maps movie_links rows", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  const rows = await repo.getVodStreams();
  assert.equal(rows.length, 1);
  assert.equal(rows[0].stream_id, 5);
  assert.equal(rows[0].name, "Movie A");
  assert.equal(rows[0].container_extension, "mp4");
  assert.equal(rows[0].poster, "https://image.tmdb.org/t/p/w500/p.jpg");
});

test("getLiveCategories returns distinct countries", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  const cats = await repo.getLiveCategories();
  assert.equal(cats.length, 2);
  assert.deepEqual(cats.map((c) => c.category_id).sort(), ["AR", "PY"]);
});

test("getLiveStreams maps all channels", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  const streams = await repo.getLiveStreams();
  assert.equal(streams.length, 2);
  assert.equal(streams[0].stream_type, "live");
  assert.equal(streams[0].stream_url, "https://h/l.m3u8");
});

test("getLiveStreams filters by category", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  const streams = await repo.getLiveStreams("AR");
  assert.equal(streams.length, 1);
  assert.equal(streams[0].name, "C2");
});

test("getVodCategories and getSeriesCategories", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  assert.deepEqual(await repo.getVodCategories(), [{ category_id: "movies", category_name: "Películas" }]);
  assert.deepEqual(await repo.getSeriesCategories(), [{ category_id: "series", category_name: "Series" }]);
});

test("getSeries maps metadata", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  const s = await repo.getSeries();
  assert.equal(s[0].series_id, 10);
  assert.equal(s[0].name, "Serie X");
});

test("getSeriesInfo groups episodes by season", async () => {
  const repo = createCatalogRepository(makeFakeSupabase(tables));
  const info = await repo.getSeriesInfo(10);
  assert.equal(info.seasons.length, 1);
  assert.equal(info.seasons[0].season_number, 1);
  assert.equal(info.episodes["1"].length, 2);
  assert.equal(info.episodes["1"][0].stream_id, 100);
  assert.equal(info.info.poster_path, "https://image.tmdb.org/t/p/w500/s.jpg");
});
