begin;
select plan(7);

insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select is((select privacy_ack_at from public.members), null, 'privacy not acknowledged before first access');
select is_empty($$ update public.members set display_name = 'Hacker', privacy_ack_at = now() returning id $$,
  'player cannot update their own member row directly');
select is_empty($$ update public.members set role = 'staff' where user_id = auth.uid() returning id $$,
  'player cannot promote themselves to staff');
select lives_ok('select public.ack_privacy()', 'player acknowledges the privacy notice');
select isnt((select privacy_ack_at from public.members), null, 'acknowledgement is recorded');

reset role;
select results_eq($$ select display_name, privacy_ack_at is null from public.members order by email $$,
  $$ values ('Luca B.', false), ('Coach Rossi', true) $$,
  'ack_privacy touches only privacy_ack_at of the caller''s row');

set local role anon;
select throws_ok('select public.ack_privacy()', '42501', null, 'anon cannot call ack_privacy');

select * from finish();
rollback;
