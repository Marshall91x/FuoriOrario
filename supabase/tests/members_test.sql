begin;
select plan(11);

-- A second team, so "staff sees only their own team" is meaningful.
insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into public.members (team_id, email, display_name, role) values
  ('00000000-0000-0000-0000-000000000002', 'altro@example.com', 'Altro', 'player');

-- Before User Created hook
select is(public.before_user_created('{"user":{"email":"Giocatore@Example.com"}}'), '{}'::jsonb,
  'hook accepts a roster email, case-insensitively');
select is(public.before_user_created('{"user":{"email":"sconosciuto@example.com"}}') -> 'error' ->> 'message', 'not_a_member',
  'hook rejects an email not in any roster');

-- First login links the account to the member
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'GIOCATORE@example.com');
select is((select user_id from public.members where email = 'giocatore@example.com'),
  '10000000-0000-0000-0000-000000000002'::uuid, 'new account is linked to the member with the same email');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select results_eq('select email::text from public.members', array['giocatore@example.com'],
  'player reads only their own member row');
select results_eq('select name from public.teams', array['Fuori Orario Basket'], 'player reads their own team');
select is_empty($$ update public.members set role = 'staff' returning id $$, 'player cannot update members');
select throws_ok($$ insert into public.members (team_id, email, display_name, role)
  values ('00000000-0000-0000-0000-000000000001', 'x@example.com', 'X', 'player') $$,
  '42501', null, 'player cannot add members');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select results_eq('select email::text from public.members order by email',
  array['giocatore@example.com', 'staff@example.com'], 'staff reads their team roster only');
select lives_ok($$ insert into public.members (team_id, email, display_name, role)
  values ('00000000-0000-0000-0000-000000000001', 'nuovo@example.com', 'Nuovo', 'player') $$,
  'staff adds a member to their team');
select throws_ok($$ insert into public.members (team_id, email, display_name, role)
  values ('00000000-0000-0000-0000-000000000002', 'y@example.com', 'Y', 'player') $$,
  '42501', null, 'staff cannot add members to another team');

-- Anonymous
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);
select is_empty('select * from public.members', 'anon reads no members');

select * from finish();
rollback;
