-- Copia settimana (ADR 0005): staff copy a player's exercises from the week before into [target], without checks.
-- Security invoker: plan_items' RLS decides who reads and who writes.
-- [seen] is how many exercises [target] had on the staff's screen: if it changed meanwhile (another staff copied or
-- edited), nothing is copied and FO002 asks to reload, so two copies at once don't duplicate the week.

create function public.copy_previous_week(player uuid, target date, seen int) returns setof public.plan_items
language plpgsql security invoker set search_path = '' as $$
begin
  -- Copies of the same week run one at a time: the second sees the first's exercises.
  perform pg_advisory_xact_lock(hashtextextended(player::text || target::text, 0));
  if (select count(*) from public.plan_items i where i.member_id = player and i.week = target) <> seen then
    raise exception 'plan_changed' using errcode = 'FO002';
  end if;
  return query
    insert into public.plan_items (team_id, member_id, week, title, category, volume, description, video_url, days, sort, created_at)
    select i.team_id, i.member_id, target, i.title, i.category, i.volume, i.description, i.video_url, i.days, i.sort,
      -- One statement shares now(): a microsecond apart keeps the copies in the old week's order.
      now() + row_number() over (order by i.sort, i.created_at) * interval '1 microsecond'
    from public.plan_items i
    where i.member_id = player and i.week = target - 7
    order by i.sort, i.created_at
    returning *;
end
$$;

revoke execute on function public.copy_previous_week from public, anon;
grant execute on function public.copy_previous_week to authenticated;
