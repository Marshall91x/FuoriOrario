package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member

class SupabaseRosterRepository : RosterRepository {
    override suspend fun members(): List<Member> = supabase.from("members").select().decodeList()

    override suspend fun add(member: Member) {
        try {
            supabase.from("members").insert(member)
        } catch (e: PostgrestRestException) {
            if (e.code == "23505") throw EmailTakenException() // unique (team_id, email)
            throw e
        }
    }
}
