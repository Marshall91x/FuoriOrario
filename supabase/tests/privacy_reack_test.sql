begin;
select plan(3);

insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');
update public.members set privacy_ack_at = now() - interval '1 day';

-- Re-run the reset migration as it was applied, on members who had already accepted
select lives_ok($$ do $do$ declare s text; begin
    foreach s in array (select statements from supabase_migrations.schema_migrations where name = 'privacy_reack') loop
      execute s;
    end loop;
  end $do$ $$, 'reset migration runs');
select is((select count(*)::int from public.members where privacy_ack_at is not null), 0,
  'every member must accept the privacy notice again');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select public.ack_privacy();
select isnt((select privacy_ack_at from public.members), null, 'player accepts the new notice');

select * from finish();
rollback;
