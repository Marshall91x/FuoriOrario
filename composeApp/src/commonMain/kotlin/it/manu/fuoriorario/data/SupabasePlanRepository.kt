package it.manu.fuoriorario.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import it.manu.fuoriorario.core.supabase
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** A `weekly_notes` row; team is filled by the database from the staff. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class WeeklyNote(
    val note: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) @SerialName("member_id") val memberId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val week: LocalDate? = null
)

/** A `plan_items` row's id with its embedded `plan_checks`. */
@Serializable
private data class ItemChecks(val id: String, @SerialName("plan_checks") val checks: List<Day>) {
    @Serializable
    data class Day(val day: Int)
}

class SupabasePlanRepository : PlanRepository {
    override suspend fun items(member: Member, week: LocalDate): List<PlanItem> = supabase.from("plan_items").select {
        filter {
            eq("member_id", member.id!!)
            eq("week", week.toString())
        }
        order("sort", Order.ASCENDING)
        order("created_at", Order.ASCENDING)
    }.decodeList()

    // RLS limits staff to their own team; the checks come embedded in each item.
    override suspend fun teamWeek(week: LocalDate): Pair<List<PlanItem>, Set<PlanCheck>> {
        val result = supabase.from("plan_items").select(Columns.raw("*, plan_checks(day)")) {
            filter { eq("week", week.toString()) }
        }
        val checks = result.decodeList<ItemChecks>().flatMap { item -> item.checks.map { PlanCheck(item.id, it.day) } }
        return result.decodeList<PlanItem>() to checks.toSet()
    }

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

    override suspend fun library(): List<LibraryExercise> = supabase.from("exercise_library").select {
        order("sort", Order.ASCENDING)
        // Two staff adding at once can share a place: always the same order anyway.
        order("id", Order.ASCENDING)
    }.decodeList()

    override suspend fun addToLibrary(exercise: LibraryExercise): LibraryExercise = mapErrors {
        supabase.from("exercise_library").insert(exercise) { select() }.decodeSingle()
    }

    override suspend fun updateInLibrary(exercise: LibraryExercise) = mapErrors {
        // Without sort: an edit never moves the exercise back from where another staff put it.
        val fields = JsonObject(Json.encodeToJsonElement(exercise).jsonObject - "sort")
        supabase.from("exercise_library").update(fields) {
            select()
            filter { eq("id", exercise.id!!) }
        }.requireRow()
    }

    override suspend fun removeFromLibrary(exercise: LibraryExercise) = mapErrors {
        supabase.from("exercise_library").delete {
            select()
            filter { eq("id", exercise.id!!) }
        }.requireRow()
    }

    override suspend fun reorderLibrary(library: List<LibraryExercise>) = mapErrors {
        supabase.postgrest.rpc(
            "reorder_library",
            buildJsonObject { put("ids", JsonArray(library.map { JsonPrimitive(it.id!!) })) }
        ).requireRow()
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
