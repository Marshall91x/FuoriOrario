begin;
select plan(9);

insert into auth.users (id, email) values ('10000000-0000-0000-0000-000000000001', 'staff@example.com');

set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select public.import_league('{"id":"40000000-0000-0000-0000-000000000001","name":"U19"}', 'ARTEMIS',
  '[{"number":"1","round":1,"leg":"FIRST","startsAt":"2026-11-09T18:30","home":"Atessa","away":"ARTEMIS","venue":null},
    {"number":"2","round":2,"leg":"FIRST","startsAt":"2026-11-16T18:30","home":"ARTEMIS","away":"Vasto","venue":null},
    {"number":"3","round":3,"leg":"FIRST","startsAt":"2026-11-23T18:30","home":"Vasto","away":"ARTEMIS","venue":null}]');

-- Away: our score is the away one.
select lives_ok($$ select public.save_game(
    jsonb_build_object('id', '30000000-0000-0000-0000-000000000001', 'date', '2026-10-10', 'opponent', 'Atessa',
      'home', false, 'our_score', 70, 'their_score', 60,
      'fixture_id', (select id from public.fixtures where number = '1')), '{}', '[]') $$,
  'staff save a live game started from a fixture');
select results_eq($$ select status, home_score, away_score, game_id from public.fixtures where number = '1' $$,
  $$ values ('PLAYED', 60, 70, '30000000-0000-0000-0000-000000000001'::uuid) $$,
  'the fixture gets the result, ours away, and the link');

select lives_ok($$ select public.save_game(
    jsonb_build_object('id', '30000000-0000-0000-0000-000000000002', 'date', '2026-10-10', 'opponent', 'Vasto',
      'home', true, 'our_score', 55, 'their_score', 50, 'note', null,
      'fixture_id', (select id from public.fixtures where number = '2')), '{}', '[]') $$, 'a home game');
select results_eq($$ select status, home_score, away_score from public.fixtures where number = '2' $$,
  $$ values ('PLAYED', 55, 50) $$, 'ours home: our score is the home one');

-- The result stays editable by hand: the referto counts. The game doesn't change.
update public.fixtures set home_score = 62 where number = '1';
select results_eq($$ select f.home_score, g.their_score from public.fixtures f join public.games g on g.id = f.game_id
    where number = '1' $$, $$ values (62, 60) $$, 'the result is edited by hand, the game stays');

-- A second live for a linked fixture leaves it alone.
select public.save_game(
  jsonb_build_object('id', '30000000-0000-0000-0000-000000000009', 'date', '2026-10-10', 'opponent', 'Atessa',
    'home', false, 'our_score', 90, 'their_score', 10,
    'fixture_id', (select id from public.fixtures where number = '1')), '{}', '[]');
select results_eq($$ select home_score, away_score, game_id from public.fixtures where number = '1' $$,
  $$ values (62, 70, '30000000-0000-0000-0000-000000000001'::uuid) $$, 'a linked fixture keeps its game and result');

-- Deleting the game unlinks the fixture, which keeps its result.
delete from public.games where id = '30000000-0000-0000-0000-000000000002';
select results_eq($$ select status, home_score, game_id from public.fixtures where number = '2' $$,
  $$ values ('PLAYED', 55, null::uuid) $$, 'deleting the game keeps the fixture and its result');

-- Deleting the fixture or the league keeps the game.
delete from public.fixtures where number = '1';
select results_eq('select count(*)::int from public.games where id = ''30000000-0000-0000-0000-000000000001''',
  $$ values (1) $$, 'deleting the fixture keeps the game');
delete from public.leagues;
select results_eq('select count(*)::int from public.games', $$ values (2) $$, 'deleting the league keeps the games');

select * from finish();
rollback;
