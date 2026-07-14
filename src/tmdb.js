const TMDB_IMG = "https://image.tmdb.org/t/p";
export function tmdbImage(path, size = "w500") {
  if (!path) return "";
  return `${TMDB_IMG}/${size}${path}`;
}

export async function tmdbDetails(token, tmdbId, type = "movie") {
  if (!token) return null;
  const url = `https://api.themoviedb.org/3/${type}/${tmdbId}`;
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!res.ok) return null;
  return res.json();
}
