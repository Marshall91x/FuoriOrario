begin;
select plan(7);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

create temp table reversed as
  select array_agg(id order by sort desc) as ids from public.exercise_library
  where team_id = '00000000-0000-0000-0000-000000000001';
create temp table other as
  select array_agg(id order by sort desc) as ids from public.exercise_library
  where team_id = '00000000-0000-0000-0000-000000000002';
grant select on reversed, other to authenticated;

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select is_empty($$ select public.reorder_library((select ids from reversed)) $$, 'players cannot reorder the library');
select is((select title from public.exercise_library order by sort limit 1), 'Palleggio a due palloni',
  'the order is unchanged');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select is((select count(*) from public.reorder_library((select ids from reversed)))::int, 10,
  'staff reorder their team''s library');
select results_eq($$ select title, sort from public.exercise_library order by sort limit 2 $$,
  $$ values ('Mobilità e allungamento', 0), ('Scivolamenti difensivi', 1) $$, 'in the given order, from 0');
select is_empty($$ select public.reorder_library((select ids from other)) $$, 'staff cannot reorder another team''s library');

-- Anonymous
reset role;
set local role anon;
select throws_ok($$ select public.reorder_library('{}') $$, '42501', null, 'anon cannot call it');

reset role;
select is((select title from public.exercise_library
  where team_id = '00000000-0000-0000-0000-000000000002' order by sort limit 1), 'Palleggio a due palloni',
  'the other team''s order is unchanged');

select * from finish();
rollback;
