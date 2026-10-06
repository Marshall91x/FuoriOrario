package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
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

    override suspend fun add(item: PlanItem): PlanItem = mapErrors {
        supabase.from("plan_items").insert(item) { select() }.decodeSingle()
    }

    override suspend fun checks(items: List<PlanItem>): Set<PlanCheck> {
        if (items.isEmpty()) return emptySet()
        return supabase.from("plan_checks").select(Columns.list("plan_item_id", "day")) {
            filter { isIn("plan_item_id", items.map { it.id!! }) }
        }.decodeList<PlanCheck>().toSet()
    }

    override suspend fun check(check: PlanCheck): Unit = mapErrors {
        supabase.from("plan_checks").insert(check)
    }

    override suspend fun uncheck(check: PlanCheck) = mapErrors {
        supabase.from("plan_checks").delete {
            select()
            filter {
                eq("plan_item_id", check.planItemId)
                eq("day", check.day)
            }
        }.requireRow()
    }
}
