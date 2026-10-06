begin;
select plan(16);

insert into public.members (id, team_id, email, display_name, role) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'altro@example.com', 'Altro', 'player');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com'),
  ('10000000-0000-0000-0000-000000000003', 'altro@example.com');
insert into public.shot_sessions (member_id, team_id, date, zones) values
  ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', current_date, '{"tl":[7,10]}');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);

select lives_ok($$ insert into public.shot_sessions (date, zones, note)
  values (current_date, '{"pit":[3,5],"tl":[7,10]}', 'Gambe stanche') $$,
  'player logs a session for themselves without ids');
select results_eq($$ select m.email::text, s.created_by from public.shot_sessions s join public.members m on m.id = s.member_id $$,
  $$ values ('giocatore@example.com', '10000000-0000-0000-0000-000000000002'::uuid) $$,
  'player reads only their own sessions, saved under their member and account');
select throws_ok($$ insert into public.shot_sessions (member_id, date, zones)
  values ('20000000-0000-0000-0000-000000000003', current_date, '{"tl":[1,1]}') $$,
  '42501', null, 'player cannot log a session for someone else');
select is_empty($$ delete from public.shot_sessions where member_id = '20000000-0000-0000-0000-000000000003' returning id $$,
  'player cannot delete someone else''s sessions');
select is_empty($$ update public.shot_sessions set note = 'x' returning id $$, 'sessions cannot be edited');

-- Database rules, same as the client's validation.
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{"pit":[6,5]}') $$,
  '23514', null, 'made cannot exceed attempted');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{}') $$,
  '23514', null, 'at least one zone');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{"pit":[0,0]}') $$,
  '23514', null, 'zones are saved only with attempts');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{"xyz":[1,2]}') $$,
  '23514', null, 'only the 10 zones');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{"pit":[-1,2]}') $$,
  '23514', null, 'no negative counts');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '{"pit":[1]}') $$,
  '23514', null, 'made and attempted both given');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date, '[1,2]') $$,
  '23514', null, 'zones is an object');
select throws_ok($$ insert into public.shot_sessions (date, zones) values (current_date + 2, '{"pit":[1,2]}') $$,
  '23514', null, 'no future dates');
select throws_ok($$ insert into public.shot_sessions (date, zones, note) values (current_date, '{"pit":[1,2]}', repeat('x', 301)) $$,
  '23514', null, 'note up to 300 characters');

select lives_ok($$ delete from public.shot_sessions $$, 'player deletes their own session');
reset role;
select is((select count(*) from public.shot_sessions)::int, 1, 'only the player''s own session was deleted');

select * from finish();
rollback;
