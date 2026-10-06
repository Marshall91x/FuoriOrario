-- Staff note for a player's week (PRD F4, ADR 0005): staff write it for a player of their team, the player reads their own.

create table public.weekly_notes (
  member_id uuid not null references public.members on delete cascade,
  -- The week's Monday.
  week date not null check (extract(isodow from week) = 1),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  note text not null check (char_length(btrim(note)) between 1 and 500),
  primary key (member_id, week)
);

alter table public.weekly_notes enable row level security;

create policy weekly_notes_select on public.weekly_notes for select to authenticated
  using (member_id = (public.my_member()).id or public.is_staff(team_id));
-- Staff only, for players of their own team.
create policy weekly_notes_insert on public.weekly_notes for insert to authenticated
  with check (
    public.is_staff(team_id)
    and exists (
      select 1 from public.members m
      where m.id = weekly_notes.member_id and m.team_id = weekly_notes.team_id and m.role = 'player'
    )
  );
create policy weekly_notes_update on public.weekly_notes for update to authenticated
  using (public.is_staff(team_id))
  with check (
    public.is_staff(team_id)
    and exists (
      select 1 from public.members m
      where m.id = weekly_notes.member_id and m.team_id = weekly_notes.team_id and m.role = 'player'
    )
  );
create policy weekly_notes_delete on public.weekly_notes for delete to authenticated
  using (public.is_staff(team_id));
