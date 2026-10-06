-- Local development data: one team, one staff, one player. Log in with these emails (codes in Mailpit).
insert into public.teams (id, name) values
  ('00000000-0000-0000-0000-000000000001', 'Fuori Orario Basket');

insert into public.members (team_id, email, display_name, jersey_number, position, role) values
  ('00000000-0000-0000-0000-000000000001', 'staff@example.com', 'Coach Rossi', null, null, 'staff'),
  ('00000000-0000-0000-0000-000000000001', 'giocatore@example.com', 'Luca B.', '7', 'Guardia', 'player');

-- The prototype's 10 default exercises.
insert into public.exercise_library (team_id, title, category, volume, description, sort)
select '00000000-0000-0000-0000-000000000001', title, category, volume, description, sort
from (values
  ('Palleggio a due palloni', 'Ball handling', '3 × 60"', 'Stessa altezza, poi alternati, poi uno alto e uno basso. Sguardo avanti, mai sulla palla.', 0),
  ('Mano debole: arresto e tiro', 'Tiro', '5 × 10 tiri', 'Due palleggi con la mano debole, arresto a un tempo, tiro dalla media. Conta i canestri.', 1),
  ('Form shooting sotto canestro', 'Tiro', '50 tiri', 'Una mano sola, a 1-2 metri. Gomito sotto la palla, chiusura del polso, tieni la posa.', 2),
  ('Tiri liberi sotto fatica', 'Tiro', '10 serie da 2', 'Uno sprint campo e ritorno, poi 2 liberi. Stessa routine ogni volta.', 3),
  ('Mikan drill', 'Footwork', '3 × 20 canestri', 'Destra-sinistra senza far toccare terra alla palla. Piedi rapidi, palla alta.', 4),
  ('Perno e ribaltamento', 'Footwork', '4 × 8 per lato', 'Ricezione in post, perno frontale e dorsale, finta e partenza. Piede perno incollato.', 5),
  ('Pliometria leggera', 'Atletica', '3 × 8 salti', 'Salti sulla panca, balzi laterali, atterraggio morbido. Mai a ginocchia in dentro.', 6),
  ('Core stability', 'Atletica', '3 giri', 'Plank 40", side plank 30" per lato, dead bug 12 ripetizioni.', 7),
  ('Scivolamenti difensivi', 'Difesa', '6 × 20"', 'Posizione bassa, piedi non si incrociano, mani attive. Recupero 40".', 8),
  ('Mobilità e allungamento', 'Recupero', '15 minuti', 'Anche, caviglie, schiena. Da fare dopo le partite e nei giorni liberi.', 9)
) as l (title, category, volume, description, sort);
