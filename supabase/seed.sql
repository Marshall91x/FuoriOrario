-- Local development data: one team (with the default library), one staff, one player, one play. Log in with these emails (codes in Mailpit).
insert into public.teams (id, name) values
  ('00000000-0000-0000-0000-000000000001', 'Fuori Orario Basket');

insert into public.members (team_id, email, display_name, jersey_number, position, role) values
  ('00000000-0000-0000-0000-000000000001', 'staff@example.com', 'Coach Rossi', null, null, 'staff'),
  ('00000000-0000-0000-0000-000000000001', 'giocatore@example.com', 'Luca B.', '7', 'Guardia', 'player');


-- One play to look at: half court, 5 screens for 1, 1 drives off it and passes to 5 rolling.
insert into public.plays (team_id, title, category, description, court, defense, steps) values
  ('00000000-0000-0000-0000-000000000001', 'Pick and roll centrale', 'Attacco',
   'Blocco del centro per il playmaker in punta, poi taglio a canestro.', 'HALF', false, '[
    {"pos":{"1":{"x":250,"y":330},"2":{"x":60,"y":230},"3":{"x":440,"y":230},"4":{"x":40,"y":40},"5":{"x":330,"y":150}},
     "ball":"1","note":"1 porta palla in punta, 5 al gomito destro."},
    {"pos":{"1":{"x":250,"y":330},"2":{"x":60,"y":230},"3":{"x":440,"y":230},"4":{"x":40,"y":40},"5":{"x":285,"y":305}},
     "ball":"1","screens":["5"],"note":"5 sale e porta il blocco sulla destra di 1."},
    {"pos":{"1":{"x":370,"y":250},"2":{"x":60,"y":230},"3":{"x":460,"y":60},"4":{"x":40,"y":40},"5":{"x":270,"y":120}},
     "ball":"1","curves":{"1":{"x":340,"y":330}},"note":"1 sfrutta il blocco in palleggio, 5 rolla verso canestro, 3 scende in angolo."},
    {"pos":{"1":{"x":370,"y":250},"2":{"x":60,"y":230},"3":{"x":460,"y":60},"4":{"x":40,"y":40},"5":{"x":270,"y":120}},
     "ball":"5","note":"Passaggio a 5 sul taglio."}
  ]');
