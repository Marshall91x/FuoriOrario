package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.github.jan.supabase.postgrest.result.PostgrestResult
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member
import kotlinx.serialization.json.JsonObject

class SupabaseRosterRepository : RosterRepository {
    override suspend fun members(): List<Member> = supabase.from("members").select().decodeList()

    override suspend fun add(member: Member): Member = mapErrors {
        supabase.from("members").insert(member) { select() }.decodeSingle()
    }

    override suspend fun update(member: Member) = mapErrors {
        supabase.from("members").update({
            set("display_name", member.displayName)
            set("jersey_number", member.jerseyNumber)
            set("position", member.position)
            set("role", member.role)
        }) { only(member) }.requireRow()
    }

    override suspend fun remove(member: Member) = mapErrors {
        supabase.from("members").delete { only(member) }.requireRow()
    }

    /** Targets [member]'s row and returns it, so [requireRow] can tell. */
    private fun PostgrestRequestBuilder.only(member: Member) {
        select()
        filter { eq("id", member.id!!) }
    }

    /** RLS filters update/delete silently: no row back means not allowed. */
    private fun PostgrestResult.requireRow() {
        if (decodeList<JsonObject>().isEmpty()) throw PermissionDeniedException()
    }

    private inline fun <T> mapErrors(block: () -> T): T = try {
        block()
    } catch (e: PostgrestRestException) {
        when (e.code) {
            "23505" -> throw EmailTakenException() // unique (team_id, email)
            "42501" -> throw PermissionDeniedException()
            "FO001" -> throw LastStaffException() // keep_one_staff trigger
            else -> throw e
        }
    }
}
