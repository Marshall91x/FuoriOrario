-- Riferimenti (PRD F5): staff change their team's expected percentage per zone, stored as fractions (0.55), above 0: at 0 every zone with shots would be hot.

create function public.valid_zone_refs(refs jsonb) returns boolean
language sql immutable set search_path = '' as $$
  -- case, not and: Postgres may evaluate and-ed conditions in any order, and the later ones throw on a wrong type.
  select case
    when jsonb_typeof(refs) <> 'object' then false
    when (select array_agg(zone order by zone) from jsonb_object_keys(refs) as zone)
      <> array['acd', 'acs', 'ald', 'als', 'cen', 'mlc', 'mld', 'mls', 'pit', 'tl'] then false
    else not exists (
      select 1 from jsonb_each(refs) as r(zone, ref)
      where case when jsonb_typeof(ref) <> 'number' then true else ref::numeric <= 0 or ref::numeric > 1 end
    )
  end
$$;

alter table public.teams add constraint teams_zone_refs_check check (public.valid_zone_refs(zone_refs));

-- Updates the caller's own team: the client doesn't know its id. Security invoker: teams' RLS lets only staff write.
create function public.set_zone_refs(refs jsonb) returns setof public.teams
language sql security invoker set search_path = '' as $$
  update public.teams set zone_refs = refs
  where id = (public.my_member()).team_id
  returning *;
$$;

revoke execute on function public.set_zone_refs from public, anon;
grant execute on function public.set_zone_refs to authenticated;
