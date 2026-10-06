package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.PlanCheck
import it.manu.fuoriorario.domain.PlanItem
import kotlinx.datetime.LocalDate

/**
 * Weekly plans: RLS lets a player read only their own, staff read and write those of their team's players.
 * Checks are the other way round: only the player checks, staff read them.
 */
interface PlanRepository {
    /** [member]'s exercises for [week] (its Monday), in the order they were added. */
    suspend fun items(member: Member, week: LocalDate): List<PlanItem>

    /** Saves [item] under its [PlanItem.memberId], returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun add(item: PlanItem): PlanItem

    /** The checks on [items]. */
    suspend fun checks(items: List<PlanItem>): Set<PlanCheck>

    /** Throws [PermissionDeniedException]. */
    suspend fun check(check: PlanCheck)

    /** Throws [PermissionDeniedException]. */
    suspend fun uncheck(check: PlanCheck)
}
