begin;
select plan(14);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');
-- Copies in a plan of the exercises staff edit and delete below.
insert into public.plan_items (member_id, team_id, week, title, category, volume, description, days)
select (select id from public.members where email = 'giocatore@example.com'), team_id, '2026-09-28', title, category,
  volume, description, '{0}'
from public.exercise_library where team_id = '00000000-0000-0000-0000-000000000001' and sort in (0, 9);
select is((select count(*) from public.exercise_library where team_id = '00000000-0000-0000-0000-000000000002')::int,
  10, 'a new team starts with the default library');
select throws_ok($$ insert into public.exercise_library (team_id, title, category)
  values ('00000000-0000-0000-0000-000000000002', 'Nuoto', 'Nuoto') $$, '23514', null, 'only the six areas');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select is((select count(*) from public.exercise_library)::int, 10, 'players read their team''s library, not others''');
select throws_ok($$ insert into public.exercise_library (title, category) values ('Mio', 'Tiro') $$,
  '42501', null, 'players cannot add to the library');
update public.exercise_library set title = 'Mio';
delete from public.exercise_library;

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select results_eq($$ select title, volume from public.exercise_library order by sort limit 2 $$,
  $$ values ('Palleggio a due palloni', '3 × 60"'), ('Mano debole: arresto e tiro', '5 × 10 tiri') $$,
  'players changed nothing; staff read the prototype''s defaults in order');
select is((select count(*) from public.exercise_library)::int, 10, 'staff read their team''s library, not others''');
select lives_ok($$ insert into public.exercise_library (title, category, sort) values ('Arresto e tiro', 'Tiro', 10) $$,
  'staff add to their team''s library');
select results_eq($$ select team_id from public.exercise_library where title = 'Arresto e tiro' $$,
  $$ values ('00000000-0000-0000-0000-000000000001'::uuid) $$, 'in the staff''s team');
select throws_ok($$ insert into public.exercise_library (team_id, title, category)
  values ('00000000-0000-0000-0000-000000000002', 'Fuori', 'Tiro') $$, '42501', null, 'not to another team''s');
select results_eq($$ update public.exercise_library set volume = '4 × 60"' where sort = 0 returning volume $$,
  $$ values ('4 × 60"') $$, 'staff edit their team''s library');
select results_eq($$ delete from public.exercise_library where sort = 9 returning title $$,
  $$ values ('Mobilità e allungamento') $$, 'staff delete from their team''s library');

-- Anonymous
reset role;
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
select is_empty('select * from public.exercise_library', 'anon reads no library');
reset role;
select is((select count(*) from public.exercise_library where team_id = '00000000-0000-0000-0000-000000000002')::int,
  10, 'the other team''s library is untouched');
select results_eq($$ select title, volume from public.plan_items order by title $$,
  $$ values ('Mobilità e allungamento', '15 minuti'), ('Palleggio a due palloni', '3 × 60"') $$,
  'plans keep their copies of edited and deleted exercises');

select * from finish();
rollback;
