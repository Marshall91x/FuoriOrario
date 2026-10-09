-- Checks on plan exercises (PRD F4): only the player the exercise is for checks, and only its assigned days;
-- staff of the team read them.

create table public.plan_checks (
  plan_item_id uuid not null references public.plan_items on delete cascade,
  -- 0 = Monday … 6 = Sunday.
  day smallint not null check (day between 0 and 6),
  checked_at timestamptz not null default now(),
  primary key (plan_item_id, day)
);

alter table public.plan_checks enable row level security;

create policy plan_checks_select on public.plan_checks for select to authenticated
  using (exists (
    select 1 from public.plan_items i
    where i.id = plan_item_id and (i.member_id = (public.my_member()).id or public.is_staff(i.team_id))
  ));
create policy plan_checks_insert on public.plan_checks for insert to authenticated
  with check (exists (
    select 1 from public.plan_items i
    where i.id = plan_item_id and i.member_id = (public.my_member()).id and day = any(i.days)
  ));
create policy plan_checks_delete on public.plan_checks for delete to authenticated
  using (exists (
    select 1 from public.plan_items i
    where i.id = plan_item_id and i.member_id = (public.my_member()).id
  ));
