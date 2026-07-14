import { tmdbImage } from "../tmdb.js";

function parseJsonArray(v) {
  try { return JSON.parse(v || "[]"); } catch { return []; }
}

export function createCatalogRepository(supabase) {
  async function all(table, select = "*") {
    const { data, error } = await supabase.from(table).select(select).order("id");
    if (error) throw error;
    return data || [];
  }

  return {
    async getLiveCategories() {
      const rows = await all("tv_channels", "pais");
      const map = new Map();
      for (const r of rows) {
        const key = r.pais || "unknown";
        if (!map.has(key)) map.set(key, { category_id: key, category_name: key });
      }
      return [...map.values()];
    },

    async getLiveStreams(categoryId) {
      const rows = categoryId
        ? await (await supabase.from("tv_channels").select("*").eq("pais", categoryId).order("id")).data || []
        : await all("tv_channels");
      return rows.map((c) => ({
        stream_id: c.id,
        name: c.canal,
        stream_type: "live",
        stream_icon: c.logo || "",
        category_id: c.pais || "",
        added: "",
        is_adult: "0",
        stream_url: c.m3u8 || "",
      }));
    },

    async getVodCategories() {
      return [{ category_id: "movies", category_name: "Películas" }];
    },

    async getVodStreams() {
      const rows = await all("movie_links");
      return rows.map((m) => ({
        stream_id: m.id,
        name: m.title,
        container_extension: "mp4",
        category_id: "movies",
        added: "",
        rating: String(m.vote_average || ""),
        poster: tmdbImage(m.poster_path, "w500"),
        plot: m.overview || "",
        release_date: m.release_date || "",
        stream_url: m.player_url || "",
        tmdb_id: m.tmdb_id,
      }));
    },

    async getSeriesCategories() {
      return [{ category_id: "series", category_name: "Series" }];
    },

    async getSeries() {
      const rows = await all("series_metadata");
      return rows.map((s) => ({
        series_id: s.id,
        name: s.title,
        category_id: "series",
        cover: tmdbImage(s.poster_path, "w500"),
        plot: s.overview || "",
        poster: tmdbImage(s.poster_path, "w500"),
        release_date: s.first_air_date || "",
        rating: String(s.vote_average || ""),
        tmdb_id: s.tmdb_id,
      }));
    },

    async getSeriesInfo(seriesId) {
      const meta = await supabase.from("series_metadata").select("*").eq("id", seriesId).maybeSingle();
      if (!meta.data) return null;
      const s = meta.data;
      const { data: eps } = await supabase
        .from("series_episodes")
        .select("*")
        .eq("tmdb_id", s.tmdb_id)
        .order("season_number")
        .order("episode_number");

      const seasonsMap = new Map();
      const episodesBySeason = {};
      for (const e of eps || []) {
        const season = e.season_number || 1;
        if (!seasonsMap.has(season)) {
          seasonsMap.set(season, { season_number: season, name: `Season ${season}`, cover: tmdbImage(s.poster_path, "w500") });
          episodesBySeason[season] = [];
        }
        episodesBySeason[season].push({
          id: e.id,
          episode_num: e.episode_number,
          title: e.title,
          container_extension: "m3u8",
          info: { plot: "", movie_image: tmdbImage(s.poster_path, "w500"), duration_secs: 0 },
          added: "",
          season,
          stream_id: e.id,
        });
      }

      return {
        seasons: [...seasonsMap.values()],
        info: {
          name: s.title,
          plot: s.overview || "",
          poster_path: tmdbImage(s.poster_path, "w500"),
          backdrop_path: tmdbImage(s.backdrop_path, "w1280"),
          genre: "",
          releaseDate: s.first_air_date || "",
          rating: String(s.vote_average || ""),
          cast: "",
          director: "",
        },
        episodes: episodesBySeason,
      };
    },
  };
}

export { parseJsonArray };
