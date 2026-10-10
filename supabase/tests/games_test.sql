begin;
select plan(19);

insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-000000000001', 'anna@example.com', 'Anna', 'player'),
  ('20000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-000000000001', 'bea@example.com', 'Bea', 'player');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-00000000000a', 'anna@example.com');

-- Staff
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select results_eq($$ select opponent, our_score, their_score from public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000001","date":"2026-10-10","opponent":"Virtus","home":true,"our_score":5,"their_score":2}',
    '{20000000-0000-0000-0000-00000000000a,20000000-0000-0000-0000-00000000000b}',
    '[{"type":"SHOT","quarter":"1","member_id":"20000000-0000-0000-0000-00000000000a","zone":"cen","made":true},
      {"type":"OPPONENT","quarter":"1","value":2},
      {"type":"FREE_THROW","quarter":"2","member_id":"20000000-0000-0000-0000-00000000000b","made":false},
      {"type":"SHOT","quarter":"5","member_id":"20000000-0000-0000-0000-00000000000b","zone":"pit","made":true}]') $$,
  $$ values ('Virtus', 5, 2) $$, 'staff save a game in one call');
select results_eq('select seq, type, quarter::int, zone from public.game_events order by seq',
  $$ values (0, 'SHOT', 1, 'cen'), (1, 'OPPONENT', 1, null), (2, 'FREE_THROW', 2, null), (3, 'SHOT', 5, 'pit') $$,
  'events saved in order with their quarter');
select results_eq('select count(*)::int from public.game_call_ups', $$ values (2) $$, 'call-ups saved with the game');

select lives_ok($$ select public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000001","date":"2026-10-10","opponent":"Virtus","home":true,"our_score":5,"their_score":2}',
    '{20000000-0000-0000-0000-00000000000a}', '[{"type":"OPPONENT","quarter":"1","value":3}]') $$,
  'saving the same game again is no error');
select results_eq('select (select count(*)::int from public.games), (select count(*)::int from public.game_events)',
  $$ values (1, 4) $$, 'saving the same game again adds nothing');

-- Checks: everything is saved or nothing.
select throws_ok($$ select public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000002","date":"2026-10-10","opponent":"X","home":true,"our_score":0,"their_score":0}',
    '{20000000-0000-0000-0000-00000000000a}',
    '[{"type":"SHOT","quarter":"1","member_id":"20000000-0000-0000-0000-00000000000a","made":true}]') $$,
  '23514', null, 'a shot needs a zone');
select throws_ok($$ select public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000002","date":"2026-10-10","opponent":"X","home":true,"our_score":0,"their_score":0}',
    '{}', '[{"type":"OPPONENT","quarter":"1","value":4}]') $$,
  '23514', null, 'the opponent scores 1 to 3 at a time');
select throws_ok($$ select public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000002","date":"2026-10-10","opponent":"X","home":true,"our_score":0,"their_score":0}',
    '{20000000-0000-0000-0000-00000000000a}',
    '[{"type":"FREE_THROW","quarter":"1","member_id":"20000000-0000-0000-0000-00000000000b","made":true}]') $$,
  '23503', null, 'only a convocato has events');
select throws_ok($$ select public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000002","date":"2026-10-10","opponent":" ","home":true,"our_score":0,"their_score":0}',
    '{}', '[]') $$,
  '23514', null, 'the game needs an opponent');
select results_eq('select count(*)::int from public.games', $$ values (1) $$, 'a failed save leaves nothing behind');

-- What the app sends: every field, nulls and zero scores included.
select results_eq($$ select our_score, their_score, note from public.save_game(
    '{"date":"2026-10-01","opponent":"Fortitudo","home":false,"note":null,"our_score":0,"their_score":3,"id":"30000000-0000-0000-0000-000000000004"}',
    '{20000000-0000-0000-0000-00000000000a,20000000-0000-0000-0000-0000000000ff}',
    '[{"type":"SHOT","quarter":"1","member_id":"20000000-0000-0000-0000-0000000000ff","zone":"cen","made":false,"value":null},
      {"type":"OPPONENT","quarter":"1","member_id":null,"zone":null,"made":null,"value":3}]') $$,
  $$ values (0, 3, null::text) $$, 'a game at nil saves');
select results_eq($$ select count(*)::int from public.game_events where game_id = '30000000-0000-0000-0000-000000000004' $$,
  $$ values (1) $$, 'a convocato no longer in the roster is left out with their events');
delete from public.games where id = '30000000-0000-0000-0000-000000000004';

-- Player
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-00000000000a"}', true);
select results_eq('select opponent, our_score, their_score from public.games', $$ values ('Virtus', 5, 2) $$,
  'players read their team''s games with the score');
select results_eq('select member_id, type from public.game_events',
  $$ values ('20000000-0000-0000-0000-00000000000a'::uuid, 'SHOT') $$, 'players read only their own events');
select throws_ok($$ select public.save_game(
    '{"id":"30000000-0000-0000-0000-000000000003","date":"2026-10-10","opponent":"Mia","home":true,"our_score":0,"their_score":0}',
    '{}', '[]') $$,
  '42501', null, 'players cannot save games');
select is_empty('delete from public.games returning id', 'players cannot delete games');

-- Removing a member
reset role;
delete from public.members where id = '20000000-0000-0000-0000-00000000000b';
select results_eq('select count(*)::int, (select our_score from public.games) from public.game_events',
  $$ values (2, 5) $$, 'removing a member removes their events, the score stays');

-- Staff delete
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select results_eq('delete from public.games returning opponent', $$ values ('Virtus') $$, 'staff delete a game');

-- Anonymous
reset role;
insert into public.games (id, team_id, date, opponent, home, our_score, their_score) values
  ('30000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000001', current_date, 'Y', false, 0, 0);
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
select is_empty('select * from public.games', 'anon reads no games');

select * from finish();
rollback;
