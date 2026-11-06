-- Calendario (ADR 0011): a league is a FIP phase, imported from its PDF. Everyone in the team reads it, staff write.

create table public.leagues (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  name text not null check (char_length(btrim(name)) between 1 and 120),
  created_at timestamptz not null default now(),
  unique (id, team_id)
);

create index on public.leagues (team_id, created_at);

create table public.league_teams (
  id uuid primary key default gen_random_uuid(),
  league_id uuid not null,
  team_id uuid not null,
  name text not null check (char_length(btrim(name)) between 1 and 120),
  ours boolean not null default false,
  unique (league_id, name),
  unique (id, league_id),
  foreign key (league_id, team_id) references public.leagues (id, team_id) on delete cascade
);

-- One team of the league is ours.
create unique index on public.league_teams (league_id) where ours;

create table public.fixtures (
  id uuid primary key default gen_random_uuid(),
  league_id uuid not null,
  team_id uuid not null,
  -- FIP number: finds the fixture again at a new import.
  number text not null check (char_length(btrim(number)) between 1 and 20),
  round smallint not null check (round between 1 and 60),
  leg text not null check (leg in ('FIRST', 'SECOND')),
  starts_at timestamptz,
  home_team_id uuid not null,
  away_team_id uuid not null check (away_team_id <> home_team_id),
  venue text check (char_length(venue) <= 200),
  status text not null default 'SCHEDULED' check (status in ('SCHEDULED', 'POSTPONED', 'PLAYED')),
  home_score int check (home_score >= 0),
  away_score int check (away_score >= 0),
  unique (league_id, number),
  foreign key (league_id, team_id) references public.leagues (id, team_id) on delete cascade,
  -- Both teams of the same league.
  foreign key (home_team_id, league_id) references public.league_teams (id, league_id),
  foreign key (away_team_id, league_id) references public.league_teams (id, league_id),
  check ((status = 'POSTPONED') = (starts_at is null)),
  check ((status = 'PLAYED') = (home_score is not null and away_score is not null)),
  check ((home_score is null) = (away_score is null))
);

create index on public.fixtures (league_id, starts_at);

alter table public.leagues enable row level security;
alter table public.league_teams enable row level security;
alter table public.fixtures enable row level security;

create policy leagues_select on public.leagues for select to authenticated
  using (team_id = (public.my_member()).team_id);
create policy leagues_insert on public.leagues for insert to authenticated
  with check (public.is_staff(team_id));
create policy leagues_update on public.leagues for update to authenticated
  using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy leagues_delete on public.leagues for delete to authenticated
  using (public.is_staff(team_id));

create policy league_teams_select on public.league_teams for select to authenticated
  using (team_id = (public.my_member()).team_id);
create policy league_teams_insert on public.league_teams for insert to authenticated
  with check (public.is_staff(team_id));
create policy league_teams_update on public.league_teams for update to authenticated
  using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy league_teams_delete on public.league_teams for delete to authenticated
  using (public.is_staff(team_id));

create policy fixtures_select on public.fixtures for select to authenticated
  using (team_id = (public.my_member()).team_id);
create policy fixtures_insert on public.fixtures for insert to authenticated
  with check (public.is_staff(team_id));
create policy fixtures_update on public.fixtures for update to authenticated
  using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy fixtures_delete on public.fixtures for delete to authenticated
  using (public.is_staff(team_id));

-- Confirms an import preview in one transaction. Security invoker: the RLS above decide, so only staff write.
-- The league id comes from the device (a new one, or the league shown when reimporting): sending it again adds
-- nothing. Fixtures are found by FIP number: a known one gets its date, time and venue, never its result; one no
-- longer in the PDF stays. Teams are found by name; `ours` marks ours.
-- league: {"id","name"}; fixtures: [{"number","round","leg","startsAt" (local Europe/Rome, null if postponed),
-- "home","away","venue"}].
create function public.import_league(league jsonb, ours text, fixtures jsonb) returns uuid
language plpgsql security invoker set search_path = '' as $$
declare
  saved public.leagues;
begin
  insert into public.leagues (id, name) values ((league ->> 'id')::uuid, league ->> 'name')
  on conflict (id) do nothing;
  select * into saved from public.leagues l where l.id = (league ->> 'id')::uuid;
  if not found then
    raise insufficient_privilege;
  end if;

  insert into public.league_teams (league_id, team_id, name)
  select distinct saved.id, saved.team_id, t
  from jsonb_array_elements(fixtures) f, lateral (values (f ->> 'home'), (f ->> 'away')) v(t)
  on conflict (league_id, name) do nothing;

  update public.league_teams t set ours = (t.name = import_league.ours) where t.league_id = saved.id and t.ours;
  update public.league_teams t set ours = true where t.league_id = saved.id and t.name = import_league.ours;

  insert into public.fixtures as x (league_id, team_id, number, round, leg, starts_at, home_team_id, away_team_id, venue,
    status)
  select saved.id, saved.team_id, f ->> 'number', (f ->> 'round')::smallint, f ->> 'leg',
    (f ->> 'startsAt')::timestamp at time zone 'Europe/Rome', h.id, a.id, f ->> 'venue',
    case when f ->> 'startsAt' is null then 'POSTPONED' else 'SCHEDULED' end
  from jsonb_array_elements(fixtures) f
  join public.league_teams h on h.league_id = saved.id and h.name = f ->> 'home'
  join public.league_teams a on a.league_id = saved.id and a.name = f ->> 'away'
  on conflict (league_id, number) do update set
    starts_at = case when x.status = 'PLAYED' then coalesce(excluded.starts_at, x.starts_at) else excluded.starts_at end,
    venue = excluded.venue,
    status = case when x.status = 'PLAYED' then 'PLAYED' else excluded.status end;

  return saved.id;
end
$$;

revoke execute on function public.import_league from public, anon;
grant execute on function public.import_league to authenticated;
