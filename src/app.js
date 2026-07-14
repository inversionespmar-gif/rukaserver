import express from "express";

export function createApp({ auth, catalog, config, resolver, streamRouter }) {
  const app = express();
  app.disable("x-powered-by");
  app.get("/ping", (_req, res) => res.json({ status: "ok" }));
  app._xtream = { auth, catalog, config };
  app._resolver = resolver;
  app._stream = streamRouter;
  return app;
}
