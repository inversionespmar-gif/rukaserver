export function createAuth(usersRepo) {
  return {
    async authenticate(username, password) {
      if (!username || !password) {
        return { auth: 0, message: "Missing credentials" };
      }
      let user;
      try {
        user = await usersRepo.findByCredentials(username, password);
      } catch {
        return { auth: 0, message: "Database error" };
      }
      if (!user) return { auth: 0, message: "Invalid username or password" };
      if (user.status === false) return { auth: 0, message: "Account disabled" };
      const now = Math.floor(Date.now() / 1000);
      if (user.exp_date && user.exp_date < now) {
        return { auth: 0, message: "Subscription expired" };
      }
      return { auth: 1, user };
    },
  };
}
