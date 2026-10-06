begin;
select plan(6);

insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select throws_ok($$ insert into public.members (email, display_name, role) values ('x@example.com', 'X', 'player') $$,
  '42501', null, 'player cannot add a player to their team');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select lives_ok($$ insert into public.members (email, display_name, jersey_number, position, role)
  values ('Nuovo@Example.com', 'Nuovo', '0', 'Centro', 'player') $$, 'staff adds a player without team_id');
select is((select team_id from public.members where email = 'nuovo@example.com'),
  '00000000-0000-0000-0000-000000000001'::uuid, 'the new player is in the staff''s team');
select throws_ok($$ insert into public.members (email, display_name, role) values ('nuovo@example.com', 'Altro', 'player') $$,
  '23505', null, 'email already in the team is rejected, case-insensitively');
select throws_ok($$ insert into public.members (email, display_name, jersey_number, role) values ('n2@example.com', 'N2', '100', 'player') $$,
  '23514', null, 'jersey number over 99 is rejected');

-- The new player can now sign up with OTP
reset role;
select is(public.before_user_created('{"user":{"email":"nuovo@example.com"}}'), '{}'::jsonb,
  'a player added by staff can sign up');

select * from finish();
rollback;
