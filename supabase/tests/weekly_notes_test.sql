begin;
select plan(16);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'altro@example.com', 'Altro', 'player'),
  ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'vice@example.com', 'Vice', 'staff'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', 'fuori@example.com', 'Fuori', 'player');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');
insert into public.weekly_notes (member_id, team_id, week, note) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', '2026-10-05', 'Altrui'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', '2026-10-05', 'Fuori');

-- Staff
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select lives_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ((select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 'Obiettivo: 75% ai liberi') $$,
  'staff write a note for a player''s week');
select results_eq($$ select team_id from public.weekly_notes where note like 'Obiettivo%' $$,
  $$ values ('00000000-0000-0000-0000-000000000001'::uuid) $$, 'saved in the staff''s team');
select throws_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ((select id from public.members where email = 'giocatore@example.com'), '2026-10-05', 'Doppia') $$,
  '23505', null, 'one note per player and week');
select results_eq($$ select note from public.weekly_notes order by note $$,
  $$ values ('Altrui'), ('Obiettivo: 75% ai liberi') $$, 'staff read their team''s notes, not other teams''');
select throws_ok($$ insert into public.weekly_notes (member_id, team_id, week, note)
  values ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', '2026-10-05', 'X') $$,
  '42501', null, 'staff cannot write for a player of another team');
select throws_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ('20000000-0000-0000-0000-000000000007', '2026-10-05', 'X') $$,
  '42501', null, 'notes are for players, not staff');
select throws_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-06', 'X') $$,
  '23514', null, 'a week starts on Monday');
select throws_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-12', ' ') $$,
  '23514', null, 'not blank');
select throws_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ('20000000-0000-0000-0000-000000000003', '2026-10-12', repeat('x', 501)) $$,
  '23514', null, 'at most 500 characters');

-- Player
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);

select results_eq($$ select note from public.weekly_notes $$, $$ values ('Obiettivo: 75% ai liberi') $$,
  'player reads only their own notes');
select throws_ok($$ insert into public.weekly_notes (member_id, week, note)
  values ((select id from public.members where email = 'giocatore@example.com'), '2026-10-12', 'X') $$,
  '42501', null, 'player cannot write notes, not even their own');
select is_empty($$ update public.weekly_notes set note = 'Y' returning note $$, 'player cannot edit notes');
select is_empty($$ delete from public.weekly_notes returning note $$, 'player cannot remove notes');

-- Staff again
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select isnt_empty($$ update public.weekly_notes set note = 'Nuova' where note like 'Obiettivo%' returning note $$,
  'staff edit a note');
select is_empty($$ update public.weekly_notes set note = 'Y' where note = 'Fuori' returning note $$,
  'staff cannot edit another team''s notes');
select isnt_empty($$ delete from public.weekly_notes where note = 'Nuova' returning note $$, 'staff remove a note');

select * from finish();
rollback;
