begin;
select plan(10);

-- Another team with its own player, to check staff stay inside their team.
insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', 'fuori@example.com', 'Fuori', 'player'),
  ('20000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000002', 'coach2@example.com', 'Coach 2', 'staff'),
  ('20000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001', 'vice@example.com', 'Vice', 'staff');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');
insert into public.shot_sessions (member_id, team_id, date, zones) values
  ((select id from public.members where email = 'giocatore@example.com'), '00000000-0000-0000-0000-000000000001', current_date, '{"tl":[7,10]}'),
  ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', current_date, '{"tl":[1,2]}');

-- Staff
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);

select results_eq($$ select m.email::text from public.shot_sessions s join public.members m on m.id = s.member_id $$,
  $$ values ('giocatore@example.com') $$, 'staff read their players'' sessions, not other teams''');
select lives_ok($$ insert into public.shot_sessions (member_id, date, zones)
  values ((select id from public.members where email = 'giocatore@example.com'), current_date, '{"pit":[3,5]}') $$,
  'staff log a session for a player of their team');
select results_eq($$ select s.created_by, s.team_id from public.shot_sessions s join public.members m on m.id = s.member_id
  where m.email = 'giocatore@example.com' and s.zones ? 'pit' $$,
  $$ values ('10000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000001'::uuid) $$,
  'saved under the player, in the team, with the staff as author');
select throws_ok($$ insert into public.shot_sessions (member_id, date, zones)
  values ('20000000-0000-0000-0000-000000000009', current_date, '{"pit":[1,2]}') $$,
  '42501', null, 'staff cannot log for a player of another team');
select throws_ok($$ insert into public.shot_sessions (member_id, team_id, date, zones)
  values ('20000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000002', current_date, '{"pit":[1,2]}') $$,
  '42501', null, 'not even naming the other team');
select throws_ok($$ insert into public.shot_sessions (member_id, date, zones)
  values ('20000000-0000-0000-0000-000000000007', current_date, '{"pit":[1,2]}') $$,
  '42501', null, 'staff log for players, not for another staff member');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{"pit":[1,2]}') $$,
  '42501', null, 'staff have no sessions of their own');
select is_empty($$ delete from public.shot_sessions where member_id = '20000000-0000-0000-0000-000000000009' returning id $$,
  'staff cannot delete another team''s sessions');
select isnt_empty($$ delete from public.shot_sessions where zones ? 'tl' returning id $$, 'staff delete a player''s session');
reset role;
select is((select count(*) from public.shot_sessions)::int, 2, 'only that session was deleted');

select * from finish();
rollback;
