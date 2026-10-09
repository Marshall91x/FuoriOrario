package it.manu.fuoriorario.feature.shots.data

import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Zone
import kotlinx.datetime.LocalDate

/** Shot sessions: RLS lets a player reach only their own, staff those of their team. */
interface ShotRepository {
    /** [member]'s sessions, newest first. */
    suspend fun sessions(member: Member): List<ShotSession>

    /** Every session of the staff's team from [since], in one request; each has its [ShotSession.memberId]. */
    suspend fun teamSessions(since: LocalDate): List<ShotSession>

    /** The team's reference percentage per zone. */
    suspend fun zoneRefs(): Map<Zone, Int>

    /** Replaces the team's riferimenti, every zone. Staff only: throws [PermissionDeniedException]. */
    suspend fun setZoneRefs(refs: Map<Zone, Int>)

    /** Saves [session] under its [ShotSession.memberId], returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun add(session: ShotSession): ShotSession

    /** Throws [PermissionDeniedException]. */
    suspend fun delete(session: ShotSession)
}
