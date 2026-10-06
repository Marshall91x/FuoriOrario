begin;
select plan(12);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'altro@example.com', 'Altro', 'player'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000002', 'coach2@example.com', 'Coach 2', 'staff');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com'),
  ('10000000-0000-0000-0000-000000000008', 'coach2@example.com');
insert into public.plan_items (id, member_id, team_id, week, title, category, days) values
  ('30000000-0000-0000-0000-000000000001', (select id from public.members where email = 'giocatore@example.com'),
    '00000000-0000-0000-0000-000000000001', '2026-10-05', 'Mio', 'Tiro', '{0,2}'),
  ('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001', '2026-10-05', 'Altrui', 'Tiro', '{0}');
insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000002', 0);

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);

select lives_ok($$ insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000001', 0) $$,
  'player checks an assigned day of their own exercise');
select throws_ok($$ insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000001', 1) $$,
  '42501', null, 'not a rest day');
select throws_ok($$ insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000002', 0) $$,
  '42501', null, 'not another player''s exercise');
select results_eq($$ select day::int from public.plan_checks $$, $$ values (0) $$,
  'player reads only their own checks');
select is_empty($$ delete from public.plan_checks where plan_item_id = '30000000-0000-0000-0000-000000000002' returning day $$,
  'player cannot uncheck another player''s exercise');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select results_eq($$ select count(*)::int from public.plan_checks $$, $$ values (2) $$, 'staff read their team''s checks');
select throws_ok($$ insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000001', 2) $$,
  '42501', null, 'staff cannot check');
select is_empty($$ delete from public.plan_checks returning day $$, 'staff cannot uncheck');

-- Staff of another team
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000008"}', true);

select is_empty($$ select day from public.plan_checks $$, 'staff of another team read no checks');

-- Player again
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);

select isnt_empty($$ delete from public.plan_checks where plan_item_id = '30000000-0000-0000-0000-000000000001' returning day $$,
  'player unchecks');

-- Removing an exercise removes its checks.
reset role;
delete from public.plan_items where id = '30000000-0000-0000-0000-000000000002';
select is((select count(*) from public.plan_checks)::int, 0, 'checks go with their exercise');
select throws_ok($$ insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000001', 7) $$,
  '23514', null, 'days are Monday 0 to Sunday 6');

select * from finish();
rollback;
