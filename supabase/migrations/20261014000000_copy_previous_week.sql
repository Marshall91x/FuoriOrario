-- Copia settimana (ADR 0005): staff copy a player's exercises from the week before into [target], without checks.
-- Security invoker: plan_items' RLS decides who reads and who writes.

create function public.copy_previous_week(player uuid, target date) returns setof public.plan_items
language sql security invoker set search_path = '' as $$
  insert into public.plan_items (team_id, member_id, week, title, category, volume, description, video_url, days, sort, created_at)
  select team_id, member_id, target, title, category, volume, description, video_url, days, sort,
    -- One statement shares now(): a microsecond apart keeps the copies in the old week's order.
    now() + row_number() over (order by sort, created_at) * interval '1 microsecond'
  from public.plan_items
  where member_id = player and week = target - 7
  order by sort, created_at
  returning *
$$;

revoke execute on function public.copy_previous_week from public, anon;
grant execute on function public.copy_previous_week to authenticated;
