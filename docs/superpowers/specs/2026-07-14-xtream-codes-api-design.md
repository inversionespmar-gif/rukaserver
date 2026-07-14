# Diseño: API Xtream Codes (Node + Express)

**Fecha:** 2026-07-14
**Estado:** Aprobado (pendiente de implementación)

## Objetivo

Construir un servidor compatible con el protocolo **Xtream Codes** para que cualquier
reproductor IPTV (TiviMate, IPTV Smarters, IPTV Player Pro, etc.) pueda conectarse usando
`servidor` + `usuario` + `contraseña` y reproducir:

- TV en vivo (`tv_channels`)
- Películas VOD (`movie_links`)
- Series (`series_metadata` + `series_episodes`)

El catálogo vive en **Supabase** y los metadatos/pósters ya provienen de **TMDB** (guardados
en la BD). El reto central es que las URLs de películas/series son **embeds** (no streams
directos) y deben resolverse a streams reales y proxyarse.

## 1. Arquitectura

Servidor Express único que:

1. Expone el protocolo Xtream Codes (`player_api.php` + rutas `/live`, `/movie`, `/series`).
2. Lee catálogos desde Supabase (`tv_channels`, `movie_links`, `series_metadata`, `series_episodes`).
3. Resuelve embeds → streams reales (m3u8/mp4) y los **proxya** (HLS reescrito).
4. Se mantiene despierto en Render free con un self-ping.

**Stack:** Node.js + Express. Despliegue: Render (free tier).

## 2. Autenticación

Se crea una tabla `users` en Supabase:

```sql
create table public.users (
  id              bigint generated always as identity primary key,
  username        text unique not null,
  password        text not null,
  exp_date        bigint,            -- epoch segundos; null = sin expiración
  max_connections int default 1,
  is_trial        boolean default false,
  status          boolean default true,
  created_at      timestamptz default now()
);
```

Endpoint `GET /player_api.php?username=U&password=P`:

- Valida usuario/contraseña activo y no expirado.
- Devuelve:
  ```json
  {
    "user_info": {
      "auth": 1,
      "username": "U",
      "password": "P",
      "message": "",
      "exp_date": 1735689600,
      "is_trial": "0",
      "active_cons": 0,
      "max_connections": "1",
      "status": "Active"
    },
    "server_info": {
      "url": "<base_url>",
      "server_protocol": "http",
      "port": "",
      "https_port": "",
      "timezone": "UTC"
    }
  }
  ```
- Si falla: `auth: 0` con `message` explicativo ("Invalid", "Expired", "Disabled").

## 3. Endpoints Xtream (lo que piden los reproductores)

Acciones sobre `player_api.php` (además de la autenticación):

| Acción | Fuente |
|--------|--------|
| `get_live_categories` | derivado de `tv_channels` (por `pais`) |
| `get_live_streams` | `tv_channels` |
| `get_vod_categories` | categoría fija "Películas" (o por `genre_ids`) |
| `get_vod_streams` | `movie_links` |
| `get_series_categories` | categoría fija "Series" |
| `get_series` | `series_metadata` |
| `get_series_info&series_id=X` | `series_metadata` + `series_episodes` agrupados por temporada |

**IDs de stream:** `tv_channels.id` y `series_episodes.id` ya son enteros. `movie_links` y
`series_metadata` NO tienen id numérico (usan `tmdb_id` string). Se añade columna:

```sql
alter table public.movie_links    add column if not exists id bigint generated always as identity primary key;
alter table public.series_metadata add column if not exists id bigint generated always as identity primary key;
```

El `stream_id` de Xtream será ese `id`.

### Mapeo de campos (VOD/series → Xtream)
- `title` → `name`
- `poster_path` → `https://image.tmdb.org/t/p/w500{poster_path}`
- `backdrop_path` → `https://image.tmdb.org/t/p/w1280{backdrop_path}`
- `overview` → `plot`
- `vote_average` → `rating`
- `release_date` / `first_air_date` → `releasedate`
- `genre_ids` → `genre` (mapeado a texto si se desea)

## 4. Resolver de embeds (componente crítico)

`movie_links.player_url` y `series_episodes.player_urls` son arrays JSON de URLs de embed
(p. ej. `vimeos.net/embed-...`, `hlswish.com/e/...`, `voe.sx/e/...`). No son reproducibles
directamente.

`src/resolver/index.js`:
1. Recibe el array de embeds.
2. Por cada URL, delega al módulo del host correspondiente (`src/resolver/hosts/*.js`).
3. El resolver **genérico** hace fetch del HTML y extrae la primera URL `.m3u8`/`.mp4` vía
   regex sobre `<source>`, `file:`, o JSON del player.
4. Resolvers **específicos** por host para casos donde el genérico falla (vimeos, hlswish,
   voe, goodstream, etc.).
5. Prueba cada fuente hasta que una resuelva; devuelve `{url, type}` (m3u8 | mp4).
6. **Cacheo** en memoria (`src/cache.js`) por `tmdb_id` o `episode_id` con TTL (~1h) para no
   re-scrapear en cada play.

## 5. Proxy de streams

Rutas (todas validan auth primero):
- `GET /live/USER/PASS/ID.m3u8`  → `tv_channels.m3u8` (URL directa HLS)
- `GET /movie/USER/PASS/ID.mp4`  → embed resuelto de `movie_links`
- `GET /series/USER/PASS/ID.m3u8` → embed resuelto de `series_episodes`

Comportamiento:
- **Live (m3u8 directo):** proxy HLS → descargo el `.m3u8`, reescribo sus segmentos `.ts`/`.m3u8`
  para que apunten a `/proxy/<encoded>` y proxyo cada segmento. Evita bloqueos de referer/CORS.
- **VOD/Series:** resuelvo el embed; si es `.m3u8` → proxy HLS; si es `.mp4` → proxy (stream
  binario) o 302 redirect.
- Ruta auxiliar `GET /proxy/*` sirve cualquier recurso remoto como proxy transparente
  (usada por la reescritura HLS y por mp4).

Headers de origen (User-Agent, Referer) se reenvían/imitan según convenga al host.

## 6. Imágenes TMDB

Se construyen directamente desde `poster_path`/`backdrop_path` ya guardados:
- `https://image.tmdb.org/t/p/w500{poster_path}`
- `https://image.tmdb.org/t/p/w1280{backdrop_path}`

No se llama la API de TMDB en runtime. El token de lectura TMDB (en `apis.txt`) queda como
**fallback opcional** para enriquecer metadatos faltantes en una futura fase (fuera de scope).

## 7. Mantener Render despierto

- Endpoint `GET /ping` que responde `ok`.
- `setInterval` en `server.js` que auto-llama `/ping` cada ~10 min.
- Recomendación: configurar UptimeRobot (gratis) cada 5 min como respaldo.
- Nota: Render free limita horas mensuales; suficiente para uso personal.

## 8. Estructura del proyecto

```
server.js                  # arranque Express + keep-alive
src/config.js              # carga de env (SUPABASE_URL, SUPABASE_KEY, TMDB_TOKEN, PORT)
src/supabase.js            # cliente Supabase
src/auth.js                # validación de users
src/xtream.js              # player_api.php + todas las acciones
src/stream.js              # rutas /live /movie /series + proxy HLS + /proxy
src/resolver/index.js      # orquesta resolución de embeds
src/resolver/hosts/*.js    # lógica específica por host
src/cache.js               # caché en memoria (TTL)
src/tmdb.js                # (opcional) helper de imágenes/fallback
.env.example               # plantilla de variables
supabase/migration.sql     # tabla users + columnas id
README.md                  # setup + despliegue en Render
```

Variables de entorno:
```
SUPABASE_URL=https://mxgiiltevgucmrystqow.supabase.co
SUPABASE_KEY=<service_role>
TMDB_TOKEN=<token lectura>   # opcional / fallback
PORT=3000
BASE_URL=https://tu-app.onrender.com
```

⚠️ El `service_role` key otorga acceso total a la BD. Rotar después de usar y no commitear `.env`.

## 9. Manejo de errores y pruebas

- Embed no resoluble → 502 con mensaje; el reproductor prueba la siguiente fuente del array.
- Auth inválido/expirado en rutas de stream → 403.
- Validación de auth en TODAS las rutas de stream y acciones.
- **Pruebas locales:**
  - `curl` contra `/player_api.php?username=&password=` (auth OK/FAIL).
  - `curl` contra un stream `/movie/USER/PASS/ID.mp4` resuelto y verificar reproducción.
  - Prueba manual en TiviMate agregando como "Xtream Codes" con server/usuario/pass.
- Se documenta un script de smoke test en README.

## Riesgos / Notas

- Los resolvers de embed son **frágiles**: si un host cambia su HTML, hay que actualizar el
  módulo del host. Se prioriza el resolver genérico y se añaden específicos según fallos.
- Render free: límites de horas y "sleep"; mitigado con keep-alive + UptimeRobot.
- No se implementa llamada en runtime a la API de TMDB (datos ya en BD).
