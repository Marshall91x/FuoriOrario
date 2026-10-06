-- Libreria (PRD F4/F5, ADR 0005): the team's model exercises, a shortcut to fill a plan. Plans copy them, never refer to
-- them. Every team starts with the prototype's 10; staff manage them.

-- Area: one list for plan exercises and the library.
create domain public.category as text
  check (value in ('Ball handling', 'Tiro', 'Footwork', 'Atletica', 'Difesa', 'Recupero'));
alter table public.plan_items drop constraint plan_items_category_check;
alter table public.plan_items alter column category type public.category;

create table public.exercise_library (
  id uuid primary key default gen_random_uuid(),
  team_id uuid not null default (public.my_member()).team_id references public.teams on delete cascade,
  title text not null check (char_length(btrim(title)) between 1 and 60),
  category public.category not null,
  volume text check (char_length(volume) <= 40),
  description text check (char_length(description) <= 400),
  video_url text check (video_url ~ '^https://'),
  sort int not null default 0
);

create index on public.exercise_library (team_id, sort);

alter table public.exercise_library enable row level security;

create policy exercise_library_select on public.exercise_library for select to authenticated
  using (team_id = (public.my_member()).team_id);
create policy exercise_library_insert on public.exercise_library for insert to authenticated
  with check (public.is_staff(team_id));
create policy exercise_library_update on public.exercise_library for update to authenticated
  using (public.is_staff(team_id)) with check (public.is_staff(team_id));
create policy exercise_library_delete on public.exercise_library for delete to authenticated
  using (public.is_staff(team_id));

-- The prototype's 10 default exercises, for every team.
create function public.add_default_library(team uuid) returns void
language sql security definer set search_path = '' as $$
  insert into public.exercise_library (team_id, title, category, volume, description, sort)
  select team, l.title, l.category, l.volume, l.description, l.sort
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
$$;

create function public.default_library() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  perform public.add_default_library(new.id);
  return null;
end
$$;

revoke execute on function public.add_default_library from public, anon, authenticated;
revoke execute on function public.default_library from public, anon, authenticated;

create trigger teams_default_library after insert on public.teams
  for each row execute function public.default_library();

-- Teams created before this migration.
select public.add_default_library(id) from public.teams;
