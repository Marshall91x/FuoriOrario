-- Libreria (PRD F5): staff put the library in the order of [ids], in one statement, so a failure never leaves two
-- exercises on the same place. Security invoker: exercise_library's RLS decides who writes; ids it hides are skipped.

create function public.reorder_library(ids uuid[]) returns setof public.exercise_library
language sql security invoker set search_path = '' as $$
  update public.exercise_library set sort = array_position(ids, id) - 1
  where id = any(ids)
  returning *;
$$;

revoke execute on function public.reorder_library from public, anon;
grant execute on function public.reorder_library to authenticated;
