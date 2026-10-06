package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate

class SupabasePlanRepository : PlanRepository {
    override suspend fun items(member: Member, week: LocalDate): List<PlanItem> = supabase.from("plan_items").select {
        filter {
            eq("member_id", member.id!!)
            eq("week", week.toString())
        }
        order("sort", Order.ASCENDING)
        order("created_at", Order.ASCENDING)
    }.decodeList()

    override suspend fun add(item: PlanItem): PlanItem = try {
        supabase.from("plan_items").insert(item) { select() }.decodeSingle()
    } catch (e: PostgrestRestException) {
        if (e.code == "42501") throw PermissionDeniedException()
        throw e
    }
}
