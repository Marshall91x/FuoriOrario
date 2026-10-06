-- Editing and removing members (PRD F2, ADR 0006).

-- A team always keeps a staff member: neither removing nor demoting the last one. A whole team being deleted is fine.
-- ponytail: two staff removing each other at the same instant can both pass; lock the team row if that ever matters.
create function public.keep_one_staff() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if old.role = 'staff'
     and (tg_op = 'DELETE' or new.role <> 'staff' or new.team_id <> old.team_id)
     and exists (select 1 from public.teams where id = old.team_id)
     and not exists (select 1 from public.members where team_id = old.team_id and role = 'staff' and id <> old.id) then
    raise exception 'last_staff' using errcode = 'FO001';
  end if;
  return coalesce(new, old);
end
$$;

create trigger keep_one_staff before update of role, team_id or delete on public.members
  for each row execute function public.keep_one_staff();

-- Removing a member deletes their account too: no email left behind, and if re-added they sign up and get linked again.
-- Their sessions, plans, notes and checks go with the member row (`on delete cascade` on member_id, see ARCHITECTURE).
create function public.delete_member_account() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  delete from auth.users where id = old.user_id;
  return old;
end
$$;

create trigger delete_member_account after delete on public.members
  for each row when (old.user_id is not null) execute function public.delete_member_account();
