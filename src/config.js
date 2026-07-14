export function loadConfig(env = process.env) {
  const supabaseUrl = env.SUPABASE_URL;
  const supabaseKey = env.SUPABASE_KEY;
  if (!supabaseUrl || !supabaseKey) {
    throw new Error("SUPABASE_URL and SUPABASE_KEY required");
  }
  const port = Number(env.PORT || 3000);
  return {
    supabaseUrl,
    supabaseKey,
    tmdbToken: env.TMDB_TOKEN || "",
    port,
    baseUrl: env.BASE_URL || `http://localhost:${port}`,
    keepAliveMs: Number(env.KEEP_ALIVE_MS || 10 * 60 * 1000),
  };
}
