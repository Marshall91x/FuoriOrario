package it.manu.fuoriorario.ui.plan

import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate

class FakePlanRepository(vararg items: PlanItem, checks: Set<PlanCheck> = emptySet()) : PlanRepository {
    val items = items.toMutableList()
    val checks = checks.toMutableSet()

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun items(member: Member, week: LocalDate) =
        items.filter { it.memberId == member.id && it.week == week }

    override suspend fun add(item: PlanItem): PlanItem {
        failIfAsked()
        return item.copy(id = "p${items.size + 1}").also { items += it }
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
