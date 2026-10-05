-- Teams, members, member <-> account linking and their RLS. See docs/ARCHITECTURE.md.

create extension if not exists citext with schema extensions;

create table public.teams (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  zone_refs jsonb not null default '{"pit":0.55,"mls":0.40,"mlc":0.40,"mld":0.40,"acs":0.36,"als":0.33,"cen":0.33,"ald":0.33,"acd":0.36,"tl":0.70}',
  created_at timestamptz not null default now()
);

create table public.members (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null references public.teams on delete cascade,
  user_id uuid unique references auth.users on delete set null,
  email extensions.citext not null,
  display_name text not null check (length(display_name) between 1 and 40),
  jersey_number text check (jersey_number ~ '^\d{1,2}$'),
  position text,
  role text not null check (role in ('staff', 'player')),
  created_at timestamptz not null default now(),
  unique (team_id, email)
);

-- RLS helpers: security definer so policies on members don't recurse into members' own RLS.
create function public.my_member() returns public.members
language sql stable security definer set search_path = '' as $$
  select * from public.members where user_id = auth.uid()
$$;

create function public.is_staff(team uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (
    select 1 from public.members where user_id = auth.uid() and team_id = team and role = 'staff'
  )
$$;

alter table public.teams enable row level security;
alter table public.members enable row level security;

create policy teams_select on public.teams for select to authenticated
  using (id = (public.my_member()).team_id);
create policy teams_update on public.teams for update to authenticated
  using (public.is_staff(id)) with check (public.is_staff(id));

create policy members_select on public.members for select to authenticated
  using (user_id = auth.uid() or public.is_staff(team_id));
create policy members_insert on public.members for insert to authenticated
  with check (public.is_staff(team_id));
create policy members_update on public.members for update to authenticated
  using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy members_delete on public.members for delete to authenticated
  using (public.is_staff(team_id));

-- Functions below run with an empty search_path, where citext's case-insensitive `=` isn't visible: compare lower().

-- Before User Created auth hook: only emails already in a roster get an account (no orphan users).
create function public.before_user_created(event jsonb) returns jsonb
language sql stable security definer set search_path = '' as $$
  select case
    when exists (select 1 from public.members where lower(email) = lower(event -> 'user' ->> 'email'))
      then '{}'::jsonb
    else jsonb_build_object('error', jsonb_build_object('http_code', 403, 'message', 'not_a_member'))
  end
$$;

revoke execute on function public.before_user_created from public, anon, authenticated;
grant execute on function public.before_user_created to supabase_auth_admin;

-- First login: link the new account to the member with the same email.
create function public.link_member() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  update public.members set user_id = new.id where lower(email) = lower(new.email) and user_id is null;
  return new;
end
$$;

create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.link_member();
