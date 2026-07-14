create table if not exists public.users (
  id              bigint generated always as identity primary key,
  username        text unique not null,
  password        text not null,
  exp_date        bigint,
  max_connections int default 1,
  is_trial        boolean default false,
  status          boolean default true,
  created_at      timestamptz default now()
);

-- movie_links and series_metadata already have a primary key on another
-- column (e.g. tmdb_id), so we cannot add a *second* primary key.
-- Add `id` as an identity column with a UNIQUE constraint so Xtream can
-- use it as a stable numeric stream_id.
alter table public.movie_links     add column if not exists id bigint generated always as identity unique;
alter table public.series_metadata add column if not exists id bigint generated always as identity unique;
