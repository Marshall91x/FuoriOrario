package it.manu.fuoriorario.ui.plan

import it.manu.fuoriorario.data.PlanRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate

class FakePlanRepository(vararg items: PlanItem) : PlanRepository {
    val items = items.toMutableList()

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun items(member: Member, week: LocalDate) =
        items.filter { it.memberId == member.id && it.week == week }

    override suspend fun add(item: PlanItem): PlanItem {
        if (failNext) {
            failNext = false
            error("offline")
        }
        return item.copy(id = "p${items.size + 1}").also { items += it }
    }
}
