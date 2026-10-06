begin;
select plan(12);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (team_id, email, display_name, role) values
  ('00000000-0000-0000-0000-000000000002', 'altro@example.com', 'Altro', 'player');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select is_empty($$ update public.members set display_name = 'Io' where email = 'giocatore@example.com' returning id $$,
  'player cannot edit even their own row');
select is_empty($$ delete from public.members where email = 'giocatore@example.com' returning id $$,
  'player cannot remove members');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select is_empty($$ update public.members set display_name = 'X' where email = 'altro@example.com' returning id $$,
  'staff cannot edit another team''s members');
select results_eq($$ update public.members set display_name = 'Luca', jersey_number = '8', position = 'Ala'
  where email = 'giocatore@example.com' returning display_name || jersey_number || position $$,
  array['Luca8Ala'], 'staff edits name, number and position');
select throws_ok($$ update public.members set role = 'player' where email = 'staff@example.com' $$,
  'FO001', 'last_staff', 'the last staff cannot become a player');
select throws_ok($$ delete from public.members where email = 'staff@example.com' $$,
  'FO001', 'last_staff', 'the last staff cannot remove themselves');

select lives_ok($$ delete from public.members where email = 'giocatore@example.com' $$, 'staff removes a player');
select lives_ok($$ insert into public.members (email, display_name, role) values ('vice@example.com', 'Vice', 'staff') $$,
  'staff adds another staff member');
select lives_ok($$ delete from public.members where email = 'staff@example.com' $$,
  'with another staff left, staff can remove themselves');

reset role;
select is_empty($$ select 1 from auth.users where email in ('giocatore@example.com', 'staff@example.com') $$,
  'removing a member deletes their account, so they can be added back and sign up again');
select is(public.before_user_created('{"user":{"email":"giocatore@example.com"}}') -> 'error' ->> 'message', 'not_a_member',
  'a removed member cannot sign up');

-- Deleting a whole team isn't blocked by its last staff.
select lives_ok($$ delete from public.teams $$, 'teams can still be deleted');

select * from finish();
rollback;
