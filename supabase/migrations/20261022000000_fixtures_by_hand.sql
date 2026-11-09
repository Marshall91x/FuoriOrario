-- Calendario (#101): staff add a fixture by hand without knowing the team id, as for leagues.
alter table public.fixtures alter column team_id set default (public.my_member()).team_id;
