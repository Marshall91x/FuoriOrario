begin;
select plan(17);

insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-000000000001', 'anna@example.com', 'Anna', 'player');
insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra');
insert into public.members (team_id, email, display_name, role) values
  ('00000000-0000-0000-0000-000000000002', 'other@example.com', 'Other', 'staff');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-00000000000a', 'anna@example.com'),
  ('10000000-0000-0000-0000-00000000000f', 'other@example.com');

-- Staff
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select is(public.import_league('{"id":"40000000-0000-0000-0000-000000000001","name":"U19 - PRIMA FASE"}', 'ARTEMIS',
    '[{"number":"000691","round":1,"leg":"FIRST","startsAt":"2026-11-09T18:30","home":"Atessa","away":"ARTEMIS","venue":"Palestra"},
      {"number":"000692","round":1,"leg":"FIRST","startsAt":"2026-11-09T19:30","home":"Vasto","away":"Village","venue":null},
      {"number":"000700","round":2,"leg":"SECOND","startsAt":null,"home":"ARTEMIS","away":"Vasto","venue":"Pala"}]'),
  '40000000-0000-0000-0000-000000000001'::uuid, 'staff confirm an import');
select results_eq('select name, ours from public.league_teams order by name',
  $$ values ('ARTEMIS', true), ('Atessa', false), ('Vasto', false), ('Village', false) $$,
  'the teams are created, ours is marked');
select results_eq($$ select number, starts_at, status, h.name, a.name, venue from public.fixtures f
    join public.league_teams h on h.id = home_team_id join public.league_teams a on a.id = away_team_id order by number $$,
  $$ values ('000691', '2026-11-09 17:30+00'::timestamptz, 'SCHEDULED', 'Atessa', 'ARTEMIS', 'Palestra'),
            ('000692', '2026-11-09 18:30+00'::timestamptz, 'SCHEDULED', 'Vasto', 'Village', null),
            ('000700', null, 'POSTPONED', 'ARTEMIS', 'Vasto', 'Pala') $$,
  'fixtures saved with time in Rome, a fixture without date is postponed');

-- A result, then a reimport.
update public.fixtures set status = 'PLAYED', home_score = 60, away_score = 70 where number = '000691';
select lives_ok($$ select public.import_league('{"id":"40000000-0000-0000-0000-000000000001","name":"U19 - PRIMA FASE"}', 'Atessa',
    '[{"number":"000691","round":1,"leg":"FIRST","startsAt":"2026-11-10T20:00","home":"Atessa","away":"ARTEMIS","venue":"Nuova"},
      {"number":"000700","round":2,"leg":"SECOND","startsAt":"2027-02-08T18:00","home":"ARTEMIS","away":"Vasto","venue":"Pala"},
      {"number":"000800","round":3,"leg":"FIRST","startsAt":"2026-11-20T18:00","home":"Nuova Squadra","away":"Vasto","venue":null}]') $$,
  'staff reimport');
select results_eq($$ select number, starts_at, status, home_score, venue from public.fixtures order by number $$,
  $$ values ('000691', '2026-11-10 19:00+00'::timestamptz, 'PLAYED', 60, 'Nuova'),
            ('000692', '2026-11-09 18:30+00'::timestamptz, 'SCHEDULED', null, null),
            ('000700', '2027-02-08 17:00+00'::timestamptz, 'SCHEDULED', null, 'Pala'),
            ('000800', '2026-11-20 17:00+00'::timestamptz, 'SCHEDULED', null, null) $$,
  'a reimport moves date and venue, keeps the result, keeps a fixture no longer there, adds a new one');
select results_eq('select count(*)::int from public.leagues', $$ values (1) $$, 'a reimport adds no league');
select results_eq('select name from public.league_teams where ours', $$ values ('Atessa') $$, 'ours can change');
select results_eq('select count(*)::int from public.league_teams', $$ values (5) $$, 'a new team is added');
select lives_ok($$ select public.import_league('{"id":"40000000-0000-0000-0000-000000000001","name":"U19 - PRIMA FASE"}', 'Atessa',
    '[{"number":"000700","round":2,"leg":"SECOND","startsAt":"2027-02-08T18:00","home":"ARTEMIS","away":"Vasto","venue":"Pala"}]') $$,
  'the same import again is no error');
select results_eq('select count(*)::int from public.fixtures', $$ values (4) $$, 'and changes nothing');

select throws_ok($$ select public.import_league('{"id":"40000000-0000-0000-0000-000000000002","name":"X"}', 'A',
    '[{"number":"1","round":1,"leg":"FIRST","startsAt":null,"home":"A","away":"A","venue":null}]') $$,
  '23514', null, 'a team doesn''t play itself');
select results_eq('select count(*)::int from public.leagues', $$ values (1) $$, 'a failed import leaves nothing behind');

-- Player
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-00000000000a"}', true);
select results_eq('select count(*)::int from public.fixtures', $$ values (4) $$, 'players read the fixtures');
select throws_ok($$ select public.import_league('{"id":"40000000-0000-0000-0000-000000000003","name":"X"}', 'A',
    '[{"number":"1","round":1,"leg":"FIRST","startsAt":null,"home":"A","away":"B","venue":null}]') $$,
  '42501', null, 'players cannot import');
select is_empty($$ update public.fixtures set venue = 'x' returning id $$, 'players cannot change fixtures');

-- Another team's staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-00000000000f"}', true);
select is_empty('select * from public.league_teams', 'another team reads nothing');

-- Staff delete the league: everything goes.
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
delete from public.leagues;
reset role;
select results_eq('select (select count(*)::int from public.league_teams), (select count(*)::int from public.fixtures)',
  $$ values (0, 0) $$, 'deleting the league deletes its teams and fixtures');

select * from finish();
rollback;
