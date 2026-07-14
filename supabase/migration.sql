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

alter table public.movie_links     add column if not exists id bigint generated always as identity primary key;
alter table public.series_metadata add column if not exists id bigint generated always as identity primary key;
