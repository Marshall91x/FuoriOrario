-- Privacy notice acknowledgement (ADR 0006). ack_privacy() is a member's only write on their own row.

alter table public.members add column privacy_ack_at timestamptz;

create function public.ack_privacy() returns void
language sql security definer set search_path = '' as $$
  update public.members set privacy_ack_at = coalesce(privacy_ack_at, now()) where user_id = auth.uid()
$$;

revoke execute on function public.ack_privacy from public, anon;
grant execute on function public.ack_privacy to authenticated;
