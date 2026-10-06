-- Staff add members without knowing their team id: it defaults to the inserting staff's team (RLS still checks it).
alter table public.members alter column team_id set default (public.my_member()).team_id;
