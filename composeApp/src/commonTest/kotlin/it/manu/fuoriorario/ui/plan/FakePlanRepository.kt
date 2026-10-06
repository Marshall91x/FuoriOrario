package it.manu.fuoriorario.ui.plan

import it.manu.fuoriorario.data.PlanChangedException
import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class FakePlanRepository(
    vararg items: PlanItem,
    checks: Set<PlanCheck> = emptySet(),
    val library: List<LibraryExercise> = emptyList()
) : PlanRepository {
    val items = items.toMutableList()
    val checks = checks.toMutableSet()

    /** Notes by (member id, week). */
    val notes = mutableMapOf<Pair<String, LocalDate>, String>()

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun items(member: Member, week: LocalDate) =
        items.filter { it.memberId == member.id && it.week == week }

    override suspend fun add(item: PlanItem): PlanItem {
        failIfAsked()
        return item.copy(id = "p${items.size + 1}").also { items += it }
    }

    override suspend fun update(item: PlanItem) {
        failIfAsked()
        items[items.indexOfFirst { it.id == item.id }] = item
    }

    override suspend fun remove(item: PlanItem) {
        failIfAsked()
        items.removeAll { it.id == item.id }
        checks.removeAll { it.planItemId == item.id }
    }

    override suspend fun copyPreviousWeek(member: Member, week: LocalDate, seen: Int): List<PlanItem> {
        failIfAsked()
        if (items(member, week).size != seen) throw PlanChangedException()
        return items(member, week.minus(DatePeriod(days = 7))).map { item ->
            item.copy(week = week, id = "p${items.size + 1}").also { items += it }
        }
    }

    override suspend fun library() = library

    override suspend fun note(member: Member, week: LocalDate) = notes[member.id!! to week]

    override suspend fun saveNote(member: Member, week: LocalDate, note: String?) {
        failIfAsked()
        if (note == null) notes -= member.id!! to week else notes[member.id!! to week] = note
    }

    override suspend fun checks(items: List<PlanItem>) = checks.filter { c ->
        items.any { it.id == c.planItemId }
    }.toSet()

    override suspend fun check(check: PlanCheck) {
        failIfAsked()
        checks += check
    }

    override suspend fun uncheck(check: PlanCheck) {
        failIfAsked()
        checks -= check
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            error("offline")
        }
    }
}
