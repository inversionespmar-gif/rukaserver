# Ruka Xtream Codes API

Servidor compatible con el protocolo Xtream Codes para reproductores IPTV
(TiviMate, IPTV Smarters, IPTV Player Pro).

## Requisitos
- Node.js >= 18
- Un proyecto Supabase con las tablas: `tv_channels`, `movie_links`,
  `series_metadata`, `series_episodes` y la tabla `users` (ver `supabase/migration.sql`).

## Setup local
1. `npm install`
2. Copia `.env.example` a `.env` y completa las variables:
   - `SUPABASE_URL`, `SUPABASE_KEY` (service_role), `TMDB_TOKEN`, `BASE_URL`
3. Ejecuta `supabase/migration.sql` en el SQL editor de Supabase (crea `users`
   y añade columnas `id` a `movie_links`/`series_metadata`).
4. Inserta un usuario de prueba:
   ```sql
   insert into public.users (username, password, exp_date, max_connections, status)
   values ('prueba', '1234', extract(epoch from now()+interval '30 days')::bigint, 1, true);
   ```
5. `npm start`

## Probar con curl
```bash
# Auth
curl "http://localhost:3000/player_api.php?username=prueba&password=1234"

# Catálogo live
curl "http://localhost:3000/player_api.php?username=prueba&password=1234&action=get_live_streams"

# Stream de una película (id numérico de movie_links)
curl -L "http://localhost:3000/movie/prueba/1234/1.mp4"
```

## Agregar en TiviMate / Smarters
Tipo de conexión: **Xtream Codes**.
- URL/Servidor: la `BASE_URL` (ej. https://tu-app.onrender.com)
- Usuario: `prueba`
- Contraseña: `1234`

## Despliegue en Render (free)
- Crea un Web Service apuntando a este repo.
- Build: `npm install`; Start: `npm start`.
- Variables de entorno: las del `.env`.
- Render free se duerme tras inactividad: este server se auto-hace ping cada
  `KEEP_ALIVE_MS` (10 min). Recomendado además un monitor gratuito
  (UptimeRobot) cada 5 min a `https://tu-app.onrender.com/ping`.

## Notas de seguridad
- El `service_role` key da acceso total a la BD. No la compartas ni la
  commitees. Rota las credenciales de `apis.txt` si las expusiste.
- Los resolvers de embed son frágiles: si un host cambia, actualiza el módulo
  en `src/resolver/hosts/`.
- El endpoint `/proxy/*` reenvía solo URLs `http(s)`; es un vector SSRF
  potencial, así que úsalo solo con usuarios de confianza.
