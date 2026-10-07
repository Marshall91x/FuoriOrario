-- Schemi (ADR 0008): the team's plays, read by everyone in the team, written by staff. Steps are JSON, see the ADR.

create function public.valid_play_steps(steps jsonb) returns boolean
language sql immutable set search_path = '' as $$
  select jsonb_typeof(steps) = 'array'
    and jsonb_array_length(steps) between 1 and 20
    and not exists (
      select 1 from jsonb_array_elements(steps) s where char_length(s ->> 'note') > 200
    )
$$;

-- Defenders X1–X5 in any step.
create function public.play_has_defenders(steps jsonb) returns boolean
language sql immutable set search_path = '' as $$
  select exists (
    select 1 from jsonb_array_elements(steps) s, jsonb_object_keys(s -> 'pos') k where k like 'X%'
  )
$$;

create table public.plays (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  title text not null check (char_length(btrim(title)) between 1 and 60),
  category text not null check (category in
    ('Attacco', 'Contro zona', 'Rimessa laterale', 'Rimessa dal fondo', 'Fine partita', 'Transizione', 'Difesa')),
  description text check (char_length(description) <= 400),
  court text not null check (court in ('HALF', 'FULL')),
  defense boolean not null default false,
  steps jsonb not null check (public.valid_play_steps(steps)),
  created_at timestamptz not null default now(),
  check (defense or not public.play_has_defenders(steps))
);

create index on public.plays (team_id);

alter table public.plays enable row level security;

create policy plays_select on public.plays for select to authenticated
  using (team_id = (public.my_member()).team_id);
create policy plays_insert on public.plays for insert to authenticated
  with check (public.is_staff(team_id));
create policy plays_update on public.plays for update to authenticated
  using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy plays_delete on public.plays for delete to authenticated
  using (public.is_staff(team_id));
