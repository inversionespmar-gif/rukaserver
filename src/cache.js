export function createCache({ ttlMs = 60 * 60 * 1000, maxEntries = 200 } = {}) {
  const store = new Map();
  return {
    get(key) {
      const v = store.get(key);
      if (!v) return null;
      if (Date.now() > v.expires) { store.delete(key); return null; }
      return v.value;
    },
    set(key, value) {
      if (store.size >= maxEntries) {
        const firstKey = store.keys().next().value;
        store.delete(firstKey);
      }
      store.set(key, { value, expires: Date.now() + ttlMs });
    },
    clear() {
      store.clear();
    },
  };
}
