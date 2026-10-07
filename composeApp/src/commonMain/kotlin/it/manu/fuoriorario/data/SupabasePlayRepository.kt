package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.from
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Play

class SupabasePlayRepository : PlayRepository {
    // RLS shows only the signed-in member's team.
    override suspend fun plays(): List<Play> = supabase.from("plays").select().decodeList()
}
