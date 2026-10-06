-- Shot sessions (PRD F3): the player's own for now; staff access comes with picking a player (#12).

-- {"pit":[made,attempted], ...}: keys among the 10 zones, only zones with attempts, 0 <= made <= attempted, at least one.
-- CASE keeps the order of the checks, so a malformed value returns false instead of failing a cast.
create function public.valid_zones(zones jsonb) returns boolean
language sql immutable set search_path = '' as $$
  select case
    when jsonb_typeof(zones) <> 'object' or zones = '{}' then false
    else not exists (
      select 1 from jsonb_each(zones) as z(zone, shots)
      where case
        when zone not in ('pit', 'mls', 'mlc', 'mld', 'acs', 'als', 'cen', 'ald', 'acd', 'tl') then true
        when jsonb_typeof(shots) <> 'array' then true
        when jsonb_array_length(shots) <> 2 then true
        when jsonb_typeof(shots -> 0) <> 'number' or jsonb_typeof(shots -> 1) <> 'number' then true
        when (shots ->> 0) !~ '^\d{1,6}$' or (shots ->> 1) !~ '^\d{1,6}$' then true
        else (shots ->> 0)::int > (shots ->> 1)::int or (shots ->> 1)::int = 0
      end
    )
  end
$$;

create table public.shot_sessions (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  member_id uuid not null default (public.my_member()).id references public.members on delete cascade,
  -- Local date (Europe/Rome): a day of slack for the server running on UTC.
  date date not null check (date <= current_date + 1),
  zones jsonb not null check (public.valid_zones(zones)),
  note text check (char_length(note) <= 300),
  created_by uuid default auth.uid() references auth.users on delete set null,
  created_at timestamptz not null default now()
);

create index on public.shot_sessions (member_id, date);

alter table public.shot_sessions enable row level security;

create policy shot_sessions_select on public.shot_sessions for select to authenticated
  using (member_id = (public.my_member()).id);
create policy shot_sessions_insert on public.shot_sessions for insert to authenticated
  with check (
    member_id = (public.my_member()).id and team_id = (public.my_member()).team_id and created_by = auth.uid()
  );
create policy shot_sessions_delete on public.shot_sessions for delete to authenticated
  using (member_id = (public.my_member()).id);
