begin;
select plan(8);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', 'fuori@example.com', 'Fuori', 'player');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');
insert into public.plan_items (id, member_id, team_id, week, title, category, volume, days, created_at) values
  ('30000000-0000-0000-0000-000000000001', (select id from public.members where email = 'giocatore@example.com'),
    '00000000-0000-0000-0000-000000000001', '2026-09-28', 'Mikan drill', 'Footwork', '3 × 20', '{0,2}', '2026-09-28 10:00'),
  ('30000000-0000-0000-0000-000000000002', (select id from public.members where email = 'giocatore@example.com'),
    '00000000-0000-0000-0000-000000000001', '2026-09-28', 'Liberi', 'Tiro', null, '{5}', '2026-09-28 09:00'),
  ('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000009',
    '00000000-0000-0000-0000-000000000002', '2026-09-28', 'Fuori', 'Tiro', null, '{0}', '2026-09-28 09:00');
insert into public.plan_checks (plan_item_id, day) values ('30000000-0000-0000-0000-000000000001', 0);

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select throws_ok($$ select public.copy_previous_week((select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 0) $$,
  '42501', null, 'player cannot copy their own plan');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select is_empty($$ select public.copy_previous_week('20000000-0000-0000-0000-000000000009', '2026-10-05', 0) $$,
  'staff cannot copy another team''s plan');
select is_empty($$ select public.copy_previous_week((select id from public.members where email = 'giocatore@example.com'), '2026-09-28', 2) $$,
  'empty previous week: nothing copied');
select results_eq($$ select title, volume, days from public.copy_previous_week(
    (select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 0) $$,
  $$ values ('Liberi', null::text, '{5}'::smallint[]), ('Mikan drill', '3 × 20', '{0,2}') $$,
  'copies the exercises, in order');
select results_eq($$ select title from public.plan_items where week = '2026-10-05' order by sort, created_at $$,
  $$ values ('Liberi'), ('Mikan drill') $$, 'into the target week, in the same order');
select throws_ok($$ select public.copy_previous_week(
    (select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 0) $$,
  'FO002', 'plan_changed', 'the week changed since the staff saw it: no copy, no duplicates');
select is_empty($$ select 1 from public.plan_checks c join public.plan_items i on i.id = c.plan_item_id
  where i.week = '2026-10-05' $$, 'without checks');
reset role;
select is((select count(*) from public.plan_items where week = '2026-09-28')::int, 3, 'the old week is untouched');

select * from finish();
rollback;
