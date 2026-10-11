-- The privacy notice now lists game stats (ADR 0006, M7.5): everyone accepts it again at next access.

update public.members set privacy_ack_at = null;
