package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.ShotSession
import kotlinx.serialization.json.JsonObject

class SupabaseShotRepository : ShotRepository {
    override suspend fun sessions(): List<ShotSession> = supabase.from("shot_sessions").select {
        order("date", Order.DESCENDING)
        order("created_at", Order.DESCENDING)
    }.decodeList()

    override suspend fun add(session: ShotSession): ShotSession = mapErrors {
        supabase.from("shot_sessions").insert(session) { select() }.decodeSingle()
    }

    override suspend fun delete(session: ShotSession) = mapErrors {
        val deleted = supabase.from("shot_sessions").delete {
            select()
            filter { eq("id", session.id!!) }
        }.decodeList<JsonObject>()
        // RLS filters deletes silently: no row back means not allowed.
        if (deleted.isEmpty()) throw PermissionDeniedException()
    }

    private inline fun <T> mapErrors(block: () -> T): T = try {
        block()
    } catch (e: PostgrestRestException) {
        if (e.code == "42501") throw PermissionDeniedException()
        throw e
    }
}
