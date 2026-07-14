export function createUsersRepository(supabase) {
  return {
    async findByCredentials(username, password) {
      const { data, error } = await supabase
        .from("users")
        .select("id, username, password, exp_date, max_connections, is_trial, status")
        .eq("username", username)
        .eq("password", password)
        .maybeSingle();
      if (error) throw error;
      return data || null;
    },
  };
}
