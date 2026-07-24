import express from "express";

export function createApp({ auth, catalog, config, resolver, streamRouter }) {
  const app = express();
  app.disable("x-powered-by");

  app.use((_req, res, next) => {
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Range, Accept, Content-Type");
    if (_req.method === "OPTIONS") return res.sendStatus(204);
    next();
  });

  app.get("/ping", (_req, res) => res.json({ status: "ok" }));
  app._xtream = { auth, catalog, config };
  app._resolver = resolver;
  app._stream = streamRouter;
  return app;
}
