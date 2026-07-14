import { createClient } from "@supabase/supabase-js";
import { loadConfig } from "./config.js";

export function createSupabase(config = loadConfig()) {
  return createClient(config.supabaseUrl, config.supabaseKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}
