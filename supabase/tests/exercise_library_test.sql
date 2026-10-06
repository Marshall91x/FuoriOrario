begin;
select plan(6);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.exercise_library (team_id, title, category) values
  ('00000000-0000-0000-0000-000000000002', 'Fuori', 'Tiro');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select is((select count(*) from public.exercise_library)::int, 10, 'players read their team''s library, not others''');
select throws_ok($$ insert into public.exercise_library (title, category) values ('Mio', 'Tiro') $$,
  '42501', null, 'players cannot add to the library');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select results_eq($$ select title, volume from public.exercise_library order by sort limit 2 $$,
  $$ values ('Palleggio a due palloni', '3 × 60"'), ('Mano debole: arresto e tiro', '5 × 10 tiri') $$,
  'staff read their team''s library, the prototype''s defaults in order');
select is((select count(*) from public.exercise_library)::int, 10, 'not other teams''');

-- Anonymous
reset role;
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
select is_empty('select * from public.exercise_library', 'anon reads no library');
reset role;
select is((select count(*) from public.exercise_library)::int, 11, 'the other team keeps its own');

select * from finish();
rollback;
