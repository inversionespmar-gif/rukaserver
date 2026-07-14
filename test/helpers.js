import { createApp } from "../src/app.js";
import { registerXtreamRoutes } from "../src/xtream.js";
import { registerStreamRoutes } from "../src/stream.js";

export async function startApp({ auth, catalog, config, resolver }) {
  const app = createApp({ auth, catalog, config, resolver });
  registerXtreamRoutes(app, { auth, catalog, config });
  registerStreamRoutes(app, { auth, catalog, resolver, config });
  const server = app.listen(0);
  await new Promise((r) => server.once("listening", r));
  const address = server.address();
  app.origin = `http://localhost:${address.port}`;
  return { app, server, close: () => new Promise((r) => server.close(r)) };
}
