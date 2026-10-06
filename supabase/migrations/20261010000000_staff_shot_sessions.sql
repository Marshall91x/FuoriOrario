-- Staff read, log and delete sessions of any player of their team (PRD "Utenti e permessi", #12).

drop policy shot_sessions_select on public.shot_sessions;
drop policy shot_sessions_insert on public.shot_sessions;
drop policy shot_sessions_delete on public.shot_sessions;

create policy shot_sessions_select on public.shot_sessions for select to authenticated
  using (member_id = (public.my_member()).id or public.is_staff(team_id));
-- Sessions belong to players only: a player logs their own, staff any player of their team. The author is always the caller.
create policy shot_sessions_insert on public.shot_sessions for insert to authenticated
  with check (
    team_id = (public.my_member()).team_id and created_by = auth.uid()
    and (member_id = (public.my_member()).id or public.is_staff(team_id))
    and exists (
      select 1 from public.members m
      where m.id = shot_sessions.member_id and m.team_id = shot_sessions.team_id and m.role = 'player'
    )
  );
create policy shot_sessions_delete on public.shot_sessions for delete to authenticated
  using (member_id = (public.my_member()).id or public.is_staff(team_id));
