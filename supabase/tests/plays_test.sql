begin;
select plan(12);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.plays (team_id, title, category, court, steps) values
  ('00000000-0000-0000-0000-000000000002', 'Altrui', 'Attacco', 'HALF', '[{"pos":{},"ball":"1"}]');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

-- Checks
select throws_ok($$ insert into public.plays (team_id, title, category, court, steps)
  select '00000000-0000-0000-0000-000000000002', 'Lungo', 'Attacco', 'HALF', jsonb_agg('{"pos":{},"ball":"1"}'::jsonb)
  from generate_series(1, 21) $$, '23514', null, 'at most 20 steps');
select throws_ok($$ insert into public.plays (team_id, title, category, court, steps)
  values ('00000000-0000-0000-0000-000000000002', 'Vuoto', 'Attacco', 'HALF', '[]') $$, '23514', null, 'at least one step');
select throws_ok($$ insert into public.plays (team_id, title, category, court, steps)
  values ('00000000-0000-0000-0000-000000000002', repeat('a', 61), 'Attacco', 'HALF', '[{"pos":{},"ball":"1"}]') $$,
  '23514', null, 'title up to 60 characters');
select throws_ok($$ insert into public.plays (team_id, title, category, court, steps)
  values ('00000000-0000-0000-0000-000000000002', 'Nota', 'Attacco', 'HALF',
    jsonb_build_array(jsonb_build_object('pos', '{}'::jsonb, 'ball', '1', 'note', repeat('a', 201)))) $$,
  '23514', null, 'notes up to 200 characters');
select throws_ok($$ insert into public.plays (team_id, title, category, court, steps)
  values ('00000000-0000-0000-0000-000000000002', 'Zona', 'Zona 2-3', 'HALF', '[{"pos":{},"ball":"1"}]') $$,
  '23514', null, 'only the seven categories');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select results_eq('select title from public.plays', $$ values ('Pick and roll centrale') $$,
  'players read their team''s plays, not others''');
select throws_ok($$ insert into public.plays (title, category, court, steps)
  values ('Mio', 'Attacco', 'HALF', '[{"pos":{},"ball":"1"}]') $$, '42501', null, 'players cannot add plays');
update public.plays set title = 'Mio';
delete from public.plays;

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select results_eq('select title from public.plays', $$ values ('Pick and roll centrale') $$,
  'players changed nothing');
select lives_ok($$ insert into public.plays (title, category, court, defense, steps)
  values ('Box', 'Rimessa dal fondo', 'FULL', true, '[{"pos":{},"ball":"1","note":"Rimessa"}]') $$,
  'staff add plays to their team');
select results_eq($$ update public.plays set title = 'Box 2' where title = 'Box' returning team_id $$,
  $$ values ('00000000-0000-0000-0000-000000000001'::uuid) $$, 'staff edit their team''s plays');
select results_eq($$ delete from public.plays where title = 'Box 2' returning title $$,
  $$ values ('Box 2') $$, 'staff delete their team''s plays');

-- Anonymous
reset role;
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
select is_empty('select * from public.plays', 'anon reads no plays');

select * from finish();
rollback;
