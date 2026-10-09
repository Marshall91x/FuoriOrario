-- Plan exercises, one week each (PRD F4, ADR 0005): staff write them for a player of their team, the player reads their own.

create table public.plan_items (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  member_id uuid not null references public.members on delete cascade,
  -- The week's Monday.
  week date not null check (extract(isodow from week) = 1),
  title text not null check (char_length(btrim(title)) between 1 and 60),
  category text not null check (category in ('Ball handling', 'Tiro', 'Footwork', 'Atletica', 'Difesa', 'Recupero')),
  volume text check (char_length(volume) <= 40),
  description text check (char_length(description) <= 400),
  video_url text check (video_url ~ '^https://'),
  -- 0 = Monday … 6 = Sunday.
  days smallint[] not null check (cardinality(days) > 0 and days <@ '{0,1,2,3,4,5,6}'),
  sort int not null default 0,
  created_at timestamptz not null default now()
);

create index on public.plan_items (member_id, week);

alter table public.plan_items enable row level security;

create policy plan_items_select on public.plan_items for select to authenticated
  using (member_id = (public.my_member()).id or public.is_staff(team_id));
-- Staff only, for players of their own team.
create policy plan_items_insert on public.plan_items for insert to authenticated
  with check (
    public.is_staff(team_id)
    and exists (
      select 1 from public.members m
      where m.id = plan_items.member_id and m.team_id = plan_items.team_id and m.role = 'player'
    )
  );
create policy plan_items_update on public.plan_items for update to authenticated
  using (public.is_staff(team_id))
  with check (
    public.is_staff(team_id)
    and exists (
      select 1 from public.members m
      where m.id = plan_items.member_id and m.team_id = plan_items.team_id and m.role = 'player'
    )
  );
create policy plan_items_delete on public.plan_items for delete to authenticated
  using (public.is_staff(team_id));
