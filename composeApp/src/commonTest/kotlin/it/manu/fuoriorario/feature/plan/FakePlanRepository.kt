package it.manu.fuoriorario.feature.plan

import it.manu.fuoriorario.domain.LibraryExercise
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import it.manu.fuoriorario.feature.plan.data.PlanChangedException
import it.manu.fuoriorario.feature.plan.data.PlanRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.io.IOException

class FakePlanRepository(
    vararg items: PlanItem,
    checks: Set<PlanCheck> = emptySet(),
    library: List<LibraryExercise> = emptyList()
) : PlanRepository {
    val items = items.toMutableList()
    val library = library.toMutableList()
    val checks = checks.toMutableSet()

    /** Notes by (member id, week). */
    val notes = mutableMapOf<Pair<String, LocalDate>, String>()

    /** Next library load fails like a network error. */
    var failLibrary = false

    /** Next write fails like a network error. */
    var failNext = false

    /** Next week's load fails without network, so the screen offers "Riprova". */
    var failLoad = false

    override suspend fun items(member: Member, week: LocalDate): List<PlanItem> {
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return items.filter { it.memberId == member.id && it.week == week }
    }

    override suspend fun teamWeek(week: LocalDate) = items.filter { it.week == week }.let { it to checks(it) }

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

    override suspend fun library(): List<LibraryExercise> {
        if (failLibrary) {
            failLibrary = false
            error("offline")
        }
        return library.toList()
    }

    override suspend fun addToLibrary(exercise: LibraryExercise): LibraryExercise {
        failIfAsked()
        return exercise.copy(id = "l${library.size + 1}").also { library += it }
    }

    override suspend fun updateInLibrary(exercise: LibraryExercise) {
        failIfAsked()
        library[library.indexOfFirst { it.id == exercise.id }] = exercise
    }

    override suspend fun removeFromLibrary(exercise: LibraryExercise) {
        failIfAsked()
        library.removeAll { it.id == exercise.id }
    }

    override suspend fun reorderLibrary(library: List<LibraryExercise>) {
        failIfAsked()
        val reordered = library.mapIndexed { i, e -> this.library.first { it.id == e.id }.copy(sort = i) }
        this.library.clear()
        this.library += reordered
    }

    override suspend fun note(member: Member, week: LocalDate) = member.id?.let { notes[it to week] }

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
