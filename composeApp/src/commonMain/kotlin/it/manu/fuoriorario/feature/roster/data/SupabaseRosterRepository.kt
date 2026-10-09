package it.manu.fuoriorario.feature.roster.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import it.manu.fuoriorario.core.error.mapErrors
import it.manu.fuoriorario.core.error.requireRow
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member

class SupabaseRosterRepository : RosterRepository {
    private val codes = mapOf(
        "23505" to ::EmailTakenException, // unique (team_id, email)
        "FO001" to ::LastStaffException // keep_one_staff trigger
    )

    override suspend fun members(): List<Member> = supabase.from("members").select().decodeList()

    override suspend fun add(member: Member): Member = mapErrors(codes) {
        supabase.from("members").insert(member) { select() }.decodeSingle()
    }

    override suspend fun update(member: Member) = mapErrors(codes) {
        supabase.from("members").update({
            set("display_name", member.displayName)
            set("jersey_number", member.jerseyNumber)
            set("position", member.position)
            set("role", member.role)
        }) { only(member) }.requireRow()
    }

    override suspend fun remove(member: Member) = mapErrors(codes) {
        supabase.from("members").delete { only(member) }.requireRow()
    }

    /** Targets [member]'s row and returns it, so [requireRow] can tell. */
    private fun PostgrestRequestBuilder.only(member: Member) {
        select()
        filter { eq("id", member.id!!) }
    }
}
