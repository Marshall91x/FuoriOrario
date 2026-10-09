-- Partite (PRD F8, ADR 0010): staff save a whole game at the end, with its convocati and its events; then it can only
-- be deleted. Everyone in the team reads the games; a player reads only their own call-up and events.

-- Call-ups and events point at a member and a game of the same team through these.
alter table public.members add unique (id, team_id);

create table public.games (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  date date not null check (date <= current_date + 1),
  opponent text not null check (char_length(btrim(opponent)) between 1 and 60),
  home boolean not null,
  note text check (char_length(note) <= 300),
  -- Also on the game: the list doesn't read the events, and a player sees the result reading only their own.
  our_score int not null check (our_score >= 0),
  their_score int not null check (their_score >= 0),
  created_at timestamptz not null default now(),
  unique (id, team_id)
);

create index on public.games (team_id, date);

create table public.game_call_ups (
  game_id uuid not null,
  team_id uuid not null,
  member_id uuid not null,
  primary key (game_id, member_id),
  foreign key (game_id, team_id) references public.games (id, team_id) on delete cascade,
  -- Removing a member removes their call-ups and, through them, their events; the game's score stays.
  foreign key (member_id, team_id) references public.members (id, team_id) on delete cascade
);

create table public.game_events (
  id uuid primary key default gen_random_uuid(),
  game_id uuid not null,
  team_id uuid not null,
  -- Order within the game, from 0.
  seq int not null check (seq >= 0),
  member_id uuid,
  type text not null check (type in
    ('SHOT', 'FREE_THROW', 'REBOUND', 'ASSIST', 'TURNOVER', 'STEAL', 'FOUL', 'OPPONENT')),
  quarter smallint not null check (quarter between 1 and 5),
  zone text check (zone in ('pit', 'mls', 'mlc', 'mld', 'acs', 'als', 'cen', 'ald', 'acd')),
  made boolean,
  value smallint,
  unique (game_id, seq),
  foreign key (game_id, team_id) references public.games (id, team_id) on delete cascade,
  -- Only a convocato's events; the opponent's have no member, and a null skips the check.
  foreign key (game_id, member_id) references public.game_call_ups (game_id, member_id) on delete cascade,
  check (case type
    when 'OPPONENT' then member_id is null and value between 1 and 3 and zone is null and made is null
    when 'SHOT' then member_id is not null and zone is not null and made is not null and value is null
    when 'FREE_THROW' then member_id is not null and zone is null and made is not null and value is null
    else member_id is not null and zone is null and made is null and value is null
  end)
);

create index on public.game_events (member_id);

alter table public.games enable row level security;
alter table public.game_call_ups enable row level security;
alter table public.game_events enable row level security;

-- No update policies: a saved game doesn't change. Call-ups and events go with their game (cascade).
create policy games_select on public.games for select to authenticated
  using (team_id = (public.my_member()).team_id);
create policy games_insert on public.games for insert to authenticated
  with check (public.is_staff(team_id));
create policy games_delete on public.games for delete to authenticated
  using (public.is_staff(team_id));

create policy game_call_ups_select on public.game_call_ups for select to authenticated
  using (public.is_staff(team_id) or member_id = (public.my_member()).id);
create policy game_call_ups_insert on public.game_call_ups for insert to authenticated
  with check (public.is_staff(team_id));

create policy game_events_select on public.game_events for select to authenticated
  using (public.is_staff(team_id) or member_id = (public.my_member()).id);
create policy game_events_insert on public.game_events for insert to authenticated
  with check (public.is_staff(team_id));

-- "Termina partita": the game, its call-ups and its events in one transaction. Security invoker: the RLS above decide,
-- so only staff save. The id comes from the device: saving it again (the answer got lost) returns the game already
-- there instead of a second one. A convocato removed from the roster during the game is left out with their events,
-- as if removed after the save (ADR 0010): otherwise the save would fail at every retry.
create function public.save_game(game jsonb, call_ups uuid[], events jsonb) returns public.games
language plpgsql security invoker set search_path = '' as $$
declare
  saved public.games;
begin
  insert into public.games (id, date, opponent, home, note, our_score, their_score)
  values ((game ->> 'id')::uuid, (game ->> 'date')::date, game ->> 'opponent', (game ->> 'home')::boolean,
    game ->> 'note', (game ->> 'our_score')::int, (game ->> 'their_score')::int)
  on conflict (id) do nothing
  returning * into saved;
  if not found then
    select * into saved from public.games g where g.id = (game ->> 'id')::uuid;
    return saved;
  end if;

  insert into public.game_call_ups (game_id, team_id, member_id)
  select saved.id, saved.team_id, m from unnest(call_ups) m
  where exists (select 1 from public.members t where t.id = m and t.team_id = saved.team_id);

  insert into public.game_events (game_id, team_id, seq, member_id, type, quarter, zone, made, value)
  select saved.id, saved.team_id, e.i - 1, (e.v ->> 'member_id')::uuid, e.v ->> 'type', (e.v ->> 'quarter')::smallint,
    e.v ->> 'zone', (e.v ->> 'made')::boolean, (e.v ->> 'value')::smallint
  from jsonb_array_elements(events) with ordinality e(v, i)
  where e.v ->> 'member_id' is null or exists (
    select 1 from public.members t where t.id = (e.v ->> 'member_id')::uuid and t.team_id = saved.team_id
  );

  return saved;
end
$$;

revoke execute on function public.save_game from public, anon;
grant execute on function public.save_game to authenticated;
