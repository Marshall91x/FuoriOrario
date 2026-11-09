-- Calendario → live (#103): a fixture points at the game recorded live for it. The link is on the fixture, so a saved
-- game never changes (ADR 0010): deleting the game unlinks the fixture, deleting the fixture or league keeps the game.
alter table public.fixtures add column game_id uuid,
  add foreign key (game_id, team_id) references public.games (id, team_id) on delete set null (game_id);

-- As before, plus: with "fixture_id" in game, that fixture gets the link and the result as played. Only when the game is new: a retried save changes nothing, and
-- the result stays editable by hand afterwards. A fixture gone, not the team's (RLS hides it) or already linked to
-- another game is skipped: its result, maybe fixed by hand, stays.
create or replace function public.save_game(game jsonb, call_ups uuid[], events jsonb) returns public.games
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

  update public.fixtures f set game_id = saved.id
  where f.id = (game ->> 'fixture_id')::uuid and f.team_id = saved.team_id and f.game_id is null;
  update public.fixtures f set status = 'PLAYED',
    home_score = case when h.ours then saved.our_score else saved.their_score end,
    away_score = case when h.ours then saved.their_score else saved.our_score end
  from public.league_teams h
  where f.game_id = saved.id and h.id = f.home_team_id;

  return saved;
end
$$;
