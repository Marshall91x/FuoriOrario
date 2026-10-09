-- Production seed: the real team and its staff. The players are added by the staff from Rosa in the app.
-- Copy to seed.production.sql (gitignored: real emails stay out of the repo), fill it in and run it once
-- in the dashboard's SQL Editor.
-- The team trigger also creates the default exercise library.
with team as (
  insert into public.teams (name) values ('Nome della squadra') returning id
)
insert into public.members (team_id, email, display_name, role)
select team.id, staff.email, staff.display_name, 'staff'
from team, (values
  ('allenatore@example.com', 'Coach'),
  ('vice@example.com', 'Vice')
) as staff (email, display_name);
