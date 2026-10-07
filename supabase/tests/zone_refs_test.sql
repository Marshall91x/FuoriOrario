begin;
select plan(11);

insert into public.teams (id, name) values ('00000000-0000-0000-0000-000000000002', 'Altra squadra');
insert into auth.users (id, email) values
  ('10000000-0000-0000-0000-000000000001', 'staff@example.com'),
  ('10000000-0000-0000-0000-000000000002', 'giocatore@example.com');

-- Player
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002"}', true);
select is_empty($$ select public.set_zone_refs(jsonb_set(zone_refs, '{pit}', '0.3')) from public.teams $$,
  'players cannot change the riferimenti');
select is_empty($$ update public.teams set zone_refs = jsonb_set(zone_refs, '{pit}', '0.3') returning id $$,
  'not even directly');

-- Staff
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001"}', true);
select is((select count(*) from public.set_zone_refs(
  '{"pit":0.3,"mls":0.40,"mlc":0.40,"mld":0.40,"acs":0.36,"als":0.33,"cen":0.33,"ald":0.33,"acd":0.36,"tl":1}'))::int, 1,
  'staff change their team''s riferimenti');
select is((select zone_refs ->> 'pit' from public.teams), '0.3', 'and everyone reads the new value');
select is_empty($$ update public.teams set zone_refs = '{}' where id = '00000000-0000-0000-0000-000000000002' returning id $$,
  'staff cannot change another team''s');
select throws_ok($$ select public.set_zone_refs(zone_refs - 'tl') from public.teams $$, '23514', null,
  'every zone needs a riferimento');
select throws_ok($$ select public.set_zone_refs(jsonb_set(zone_refs, '{tl}', '1.5')) from public.teams $$, '23514', null,
  'a riferimento is between 0 and 1');

select throws_ok($$ select public.set_zone_refs(jsonb_set(zone_refs, '{tl}', '"0.7"')) from public.teams $$, '23514', null,
  'a riferimento is a number');
select throws_ok($$ select public.set_zone_refs('[]') $$, '23514', null, 'riferimenti are an object');

-- Anonymous
reset role;
set local role anon;
select throws_ok($$ select public.set_zone_refs('{}') $$, '42501', null, 'anon cannot call it');

reset role;
select is((select zone_refs ->> 'pit' from public.teams where id = '00000000-0000-0000-0000-000000000002'), '0.55',
  'the other team keeps its riferimenti');

select * from finish();
rollback;
