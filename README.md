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

## Despliegue en Render
Este server usa **Playwright (Chromium)** para resolver los embeds de video,
porque hosts como vimeos.net / hlswish.com / voe.sx cargan el stream con
JavaScript o están detrás de Cloudflare.

- Crea un **Web Service** apuntando a este repo.
- **Recomendado: usar Docker.** Render detecta el `Dockerfile` del repo
  automáticamente y el build corre como root, por lo que puede instalar las
  librerías del sistema de Chromium con `--with-deps` (que falla en el build
  tipo Node normal por falta de `su`). Solo crea el servicio, elige
  **Environment → Docker** y desplegá; no hace falta Build/Start Command.
- Si preferís el build tipo **Node** (no recomendado): el Build Command
  `npm install && npx playwright install --with-deps chromium` falla porque
  Render no permite `su`/`apt`. Usá `npm install && npx playwright install
  chromium` (sin `--with-deps`), pero entonces Chromium puede fallar por
  librerías faltantes. Por eso Docker es la vía correcta.
- Variables de entorno: las del `.env` (`SUPABASE_URL`, `SUPABASE_KEY`,
  `TMDB_TOKEN`, `BASE_URL`, `PORT`, `KEEP_ALIVE_MS`).

### Importante sobre el hosting
- **Render free (512 MB RAM) NO es recomendado**: Chromium necesita ~300-500 MB
  y Render free se duerme. Usa el plan **Starter (~$7/mes)** o un **VPS pequeño**
  (2 GB RAM). Si usas Render free igualmente, espera resoluciones lentas y
  posibles caídas por memoria.
- Render free se duerme tras inactividad: este server se auto-hace ping cada
  `KEEP_ALIVE_MS` (10 min). Recomendado además un monitor gratuito
  (UptimeRobot) cada 5 min a `https://tu-app.onrender.com/ping`.

## Cómo funciona la resolución de embeds
1. Se intenta un scrape ligero (regex) por host (`src/resolver/hosts/*`).
2. Si falla, se carga el embed en **Chromium headless** (`src/resolver/browser.js`)
   y se captura la primera petición `.m3u8`/`.mp4` (o el `src` del `<video>`).
3. El resultado se cachea en memoria para no re-scrapear en cada play.

Si un host deja de resolverse, ajusta el módulo correspondiente en
`src/resolver/hosts/` o afina `browser.js`.

## Notas de seguridad
- El `service_role` key da acceso total a la BD. No la compartas ni la
  commitees. Rota las credenciales de `apis.txt` si las expusiste.
- El endpoint `/proxy/*` reenvía solo URLs `http(s)`; es un vector SSRF
  potencial, así que úsalo solo con usuarios de confianza.
