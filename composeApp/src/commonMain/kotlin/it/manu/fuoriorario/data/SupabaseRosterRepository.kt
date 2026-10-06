package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member

class SupabaseRosterRepository : RosterRepository {
    override suspend fun members(): List<Member> = supabase.from("members").select().decodeList()

    override suspend fun add(member: Member): Member = mapErrors {
        supabase.from("members").insert(member) { select() }.decodeSingle()
    }

    override suspend fun update(member: Member) = mapErrors {
        val saved = supabase.from("members").update({
            set("display_name", member.displayName)
            set("jersey_number", member.jerseyNumber)
            set("position", member.position)
            set("role", member.role)
        }) {
            select()
            filter { eq("id", member.id!!) }
        }.decodeList<Member>()
        // RLS filters update/delete silently: no row back means not allowed.
        if (saved.isEmpty()) throw PermissionDeniedException()
    }

    override suspend fun remove(member: Member) = mapErrors {
        val removed = supabase.from("members").delete {
            select()
            filter { eq("id", member.id!!) }
        }.decodeList<Member>()
        if (removed.isEmpty()) throw PermissionDeniedException()
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
