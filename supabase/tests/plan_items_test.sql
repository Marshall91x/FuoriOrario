begin;
select plan(19);

-- Another player of the team and another team, to check who reads and writes what.
insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'altro@example.com', 'Altro', 'player'),
  ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'vice@example.com', 'Vice', 'staff'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', 'fuori@example.com', 'Fuori', 'player');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');
insert into public.plan_items (member_id, team_id, week, title, category, days) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', '2026-10-05', 'Altrui', 'Tiro', '{0}'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', '2026-10-05', 'Fuori', 'Tiro', '{0}');

-- Staff
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select lives_ok($$ insert into public.plan_items (member_id, week, title, category, volume, description, video_url, days)
  values ((select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 'Mikan drill', 'Footwork',
    '3 × 20', 'Piedi rapidi', 'https://example.com/v', '{0,2,4}') $$,
  'staff add an exercise to a player''s week');
select results_eq($$ select team_id from public.plan_items where title = 'Mikan drill' $$,
  $$ values ('00000000-0000-0000-0000-000000000001'::uuid) $$, 'saved in the staff''s team');
select results_eq($$ select title from public.plan_items order by title $$,
  $$ values ('Altrui'), ('Mikan drill') $$, 'staff read their team''s plans, not other teams''');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000009', '2026-10-05', 'X', 'Tiro', '{0}') $$,
  '42501', null, 'staff cannot plan for a player of another team');
select throws_ok($$ insert into public.plan_items (member_id, team_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', '2026-10-05', 'X', 'Tiro', '{0}') $$,
  '42501', null, 'not even naming the other team');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000007', '2026-10-05', 'X', 'Tiro', '{0}') $$,
  '42501', null, 'plans are for players, not staff');

-- Database rules, same as the client's validation.
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-06', 'X', 'Tiro', '{0}') $$,
  '23514', null, 'a week starts on Monday');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-05', ' ', 'Tiro', '{0}') $$,
  '23514', null, 'title required');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-05', 'X', 'Tiro', '{}') $$,
  '23514', null, 'at least one day');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-05', 'X', 'Tiro', '{7}') $$,
  '23514', null, 'days are Monday 0 to Sunday 6');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, video_url, days)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-05', 'X', 'Tiro', 'http://example.com', '{0}') $$,
  '23514', null, 'video starts with https://');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-05', 'X', 'Nuoto', '{0}') $$,
  '23514', null, 'one of the six areas');

-- Player
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);

select results_eq($$ select title from public.plan_items $$, $$ values ('Mikan drill') $$,
  'player reads only their own plan');
select throws_ok($$ insert into public.plan_items (member_id, week, title, category, days)
  values ((select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 'X', 'Tiro', '{0}') $$,
  '42501', null, 'player cannot add exercises, not even to their own plan');
select is_empty($$ update public.plan_items set title = 'Y' returning id $$, 'player cannot edit exercises');
select is_empty($$ delete from public.plan_items returning id $$, 'player cannot remove exercises');

-- Staff again
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select isnt_empty($$ update public.plan_items set title = 'Mikan' where title = 'Mikan drill' returning id $$,
  'staff edit an exercise');
select isnt_empty($$ delete from public.plan_items where title = 'Mikan' returning id $$, 'staff remove an exercise');
reset role;
select is((select count(*) from public.plan_items)::int, 2, 'only that exercise was removed');

select * from finish();
rollback;
