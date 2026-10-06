-- Local development data: one team (with the default library), one staff, one player. Log in with these emails (codes in Mailpit).
insert into public.teams (id, name) values
  ('00000000-0000-0000-0000-000000000001', 'Fuori Orario Basket');

insert into public.members (team_id, email, display_name, jersey_number, position, role) values
  ('00000000-0000-0000-0000-000000000001', 'staff@example.com', 'Coach Rossi', null, null, 'staff'),
  ('00000000-0000-0000-0000-000000000001', 'giocatore@example.com', 'Luca B.', '7', 'Guardia', 'player');

