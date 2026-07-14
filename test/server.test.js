import { test } from "node:test";
import assert from "node:assert/strict";
import { createApp } from "../src/app.js";
import { registerXtreamRoutes } from "../src/xtream.js";
import { registerStreamRoutes } from "../src/stream.js";
import { createAuth } from "../src/auth.js";
import { createCache } from "../src/cache.js";
import { createDefaultResolver } from "../src/resolver/index.js";

test("/ping responds ok", async () => {
  const app = createApp({ auth: createAuth({}), catalog: {}, config: {}, resolver: createDefaultResolver({ cache: createCache() }) });
  registerXtreamRoutes(app, { auth: createAuth({}), catalog: {}, config: {} });
  registerStreamRoutes(app, { auth: createAuth({}), catalog: {}, resolver: createDefaultResolver({ cache: createCache() }), config: {} });
  const server = app.listen(0);
  await new Promise((r) => server.once("listening", r));
  const port = server.address().port;
  const res = await fetch(`http://localhost:${port}/ping`);
  const body = await res.json();
  assert.equal(body.status, "ok");
  await new Promise((r) => server.close(r));
});
