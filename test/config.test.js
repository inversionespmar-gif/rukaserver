import { test } from "node:test";
import assert from "node:assert/strict";
import { loadConfig } from "../src/config.js";

test("loadConfig returns parsed config", () => {
  const cfg = loadConfig({
    SUPABASE_URL: "https://x.supabase.co",
    SUPABASE_KEY: "k",
    PORT: "4000",
    BASE_URL: "https://app.onrender.com",
  });
  assert.equal(cfg.supabaseUrl, "https://x.supabase.co");
  assert.equal(cfg.supabaseKey, "k");
  assert.equal(cfg.port, 4000);
  assert.equal(cfg.baseUrl, "https://app.onrender.com");
});

test("loadConfig throws when supabase vars missing", () => {
  assert.throws(() => loadConfig({}), /SUPABASE_URL and SUPABASE_KEY required/);
});
