import { test } from "node:test";
import assert from "node:assert/strict";
import { createCatalogRepository } from "../src/repositories/catalog.js";
import { tmdbImage } from "../src/tmdb.js";

test("tmdbImage builds poster and backdrop urls", () => {
  assert.equal(tmdbImage("/a.jpg", "w500"), "https://image.tmdb.org/t/p/w500/a.jpg");
  assert.equal(tmdbImage("/a.jpg", "w1280"), "https://image.tmdb.org/t/p/w1280/a.jpg");
  assert.equal(tmdbImage("", "w500"), "");
});

test("getVodStreams maps movie_links rows", async () => {
  const fakeSupabase = {
    from(table) {
      return {
        select: () => ({
          order: async () => ({
            data: [
              { id: 5, tmdb_id: "1", title: "Movie A", poster_path: "/p.jpg",
                backdrop_path: "/b.jpg", overview: "plot", vote_average: 7.5,
                release_date: "2024-01-01", genre_ids: "[]", popularity: 9 },
            ],
            error: null,
          }),
        }),
      };
    },
  };
  const repo = createCatalogRepository(fakeSupabase);
  const rows = await repo.getVodStreams();
  assert.equal(rows.length, 1);
  assert.equal(rows[0].stream_id, 5);
  assert.equal(rows[0].name, "Movie A");
  assert.equal(rows[0].container_extension, "mp4");
  assert.equal(rows[0].poster, "https://image.tmdb.org/t/p/w500/p.jpg");
});
