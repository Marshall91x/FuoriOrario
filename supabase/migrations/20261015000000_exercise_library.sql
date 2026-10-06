-- Libreria (PRD F4, ADR 0005): the team's model exercises, a shortcut to fill a plan. Plans copy them, never refer to
-- them. Staff manage it in Settings (F5); until then it holds the seeded defaults.

create table public.exercise_library (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  title text not null check (char_length(btrim(title)) between 1 and 60),
  category text not null check (category in ('Ball handling', 'Tiro', 'Footwork', 'Atletica', 'Difesa', 'Recupero')),
  volume text check (char_length(volume) <= 40),
  description text check (char_length(description) <= 400),
  video_url text check (video_url ~ '^https://'),
  sort int not null default 0
);

create index on public.exercise_library (team_id, sort);

alter table public.exercise_library enable row level security;

create policy exercise_library_select on public.exercise_library for select to authenticated
  using (team_id = (public.my_member()).team_id);
