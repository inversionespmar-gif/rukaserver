import { createSupabase } from "./src/supabase.js";
import { loadConfig } from "./src/config.js";
import { createApp } from "./src/app.js";
import { registerXtreamRoutes } from "./src/xtream.js";
import { registerStreamRoutes } from "./src/stream.js";
import { createAuth } from "./src/auth.js";
import { createUsersRepository } from "./src/repositories/users.js";
import { createCatalogRepository } from "./src/repositories/catalog.js";
import { createCache } from "./src/cache.js";
import { createDefaultResolver } from "./src/resolver/index.js";

const config = loadConfig();
const supabase = createSupabase(config);
const auth = createAuth(createUsersRepository(supabase));
const catalog = createCatalogRepository(supabase);
const resolver = createDefaultResolver({ cache: createCache({ ttlMs: 60 * 60 * 1000 }) });

const app = createApp({ auth, catalog, config, resolver });
registerXtreamRoutes(app, { auth, catalog, config });
registerStreamRoutes(app, { auth, catalog, resolver, config });

const server = app.listen(config.port, () => {
  console.log(`Xtream API listening on ${config.port}`);
});

const ping = () => {
  const url = `${config.baseUrl.replace(/\/$/, "")}/ping`;
  fetch(url).catch(() => {});
};
setInterval(ping, config.keepAliveMs);

process.on("SIGTERM", () => server.close(() => process.exit(0)));
