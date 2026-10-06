package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A `weekly_notes` row; team is filled by the database from the staff. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class WeeklyNote(
    val note: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("member_id") val memberId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val week: LocalDate? = null
)

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

    override suspend fun update(item: PlanItem) = mapErrors {
        supabase.from("plan_items").update(item) {
            select()
            filter { eq("id", item.id!!) }
        }.requireRow()
    }

    override suspend fun remove(item: PlanItem) = mapErrors {
        supabase.from("plan_items").delete {
            select()
            filter { eq("id", item.id!!) }
        }.requireRow()
    }

    override suspend fun copyPreviousWeek(member: Member, week: LocalDate, seen: Int): List<PlanItem> = mapErrors(
        mapOf("FO002" to ::PlanChangedException)
    ) {
        supabase.postgrest.rpc(
            "copy_previous_week",
            buildJsonObject {
                put("player", member.id!!)
                put("target", week.toString())
                put("seen", seen)
            }
        ).decodeList()
    }

    override suspend fun note(member: Member, week: LocalDate): String? =
        supabase.from("weekly_notes").select(Columns.list("note")) {
            filter {
                eq("member_id", member.id!!)
                eq("week", week.toString())
            }
        }.decodeSingleOrNull<WeeklyNote>()?.note

    override suspend fun saveNote(member: Member, week: LocalDate, note: String?) = mapErrors {
        if (note == null) {
            supabase.from("weekly_notes").delete {
                select()
                filter {
                    eq("member_id", member.id!!)
                    eq("week", week.toString())
                }
            }.requireRow()
        } else {
            supabase.from("weekly_notes").upsert(WeeklyNote(note, member.id, week)) { select() }.requireRow()
        }
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
