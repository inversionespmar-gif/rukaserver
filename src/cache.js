export function createCache({ ttlMs = 60 * 60 * 1000 } = {}) {
  const store = new Map();
  return {
    get(key) {
      const v = store.get(key);
      if (!v) return null;
      if (Date.now() > v.expires) { store.delete(key); return null; }
      return v.value;
    },
    set(key, value) {
      store.set(key, { value, expires: Date.now() + ttlMs });
    },
  };
}
