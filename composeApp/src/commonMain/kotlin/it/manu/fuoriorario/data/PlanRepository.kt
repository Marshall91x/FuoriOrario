package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate

/** Weekly plans: RLS lets a player read only their own, staff read and write those of their team's players. */
interface PlanRepository {
    /** [member]'s exercises for [week] (its Monday), in the order they were added. */
    suspend fun items(member: Member, week: LocalDate): List<PlanItem>

    /** Saves [item] under its [PlanItem.memberId], returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun add(item: PlanItem): PlanItem
}
