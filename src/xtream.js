function buildUserInfo(user, baseUrl, password) {
  return {
    auth: 1,
    username: user.username,
    password: password || "",
    message: "",
    exp_date: user.exp_date || "",
    is_trial: user.is_trial ? "1" : "0",
    active_cons: "0",
    max_connections: String(user.max_connections || 1),
    created_at: "",
    status: "Active",
  };
}

export function registerXtreamRoutes(app, { auth, catalog, config }) {
  app.get("/player_api.php", async (req, res) => {
    const { username, password, action } = req.query;
    const result = await auth.authenticate(username, password);
    if (result.auth !== 1) {
      return res.json({ user_info: { auth: 0, message: result.message }, server_info: {} });
    }
    const user = result.user;

    if (!action) {
      return res.json({
        user_info: buildUserInfo(user, config.baseUrl, password),
        server_info: {
          url: config.baseUrl,
          server_protocol: config.baseUrl.startsWith("https") ? "https" : "http",
          port: "",
          https_port: "",
          timezone: "UTC",
        },
      });
    }

    try {
      switch (action) {
        case "get_live_categories": return res.json(await catalog.getLiveCategories());
        case "get_live_streams": return res.json(await catalog.getLiveStreams(req.query.category_id));
        case "get_vod_categories": return res.json(await catalog.getVodCategories());
        case "get_vod_streams": return res.json(await catalog.getVodStreams());
        case "get_series_categories": return res.json(await catalog.getSeriesCategories());
        case "get_series": return res.json(await catalog.getSeries());
        case "get_series_info": {
          const info = await catalog.getSeriesInfo(Number(req.query.series_id));
          return res.json(info || { seasons: [], info: {}, episodes: {} });
        }
        default: return res.json({});
      }
    } catch (e) {
      return res.status(500).json({ error: "catalog_error" });
    }
  });
}
