package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Zone

/** Shot sessions: RLS lets a player reach only their own, staff those of their team. */
interface ShotRepository {
    /** [member]'s sessions, newest first. */
    suspend fun sessions(member: Member): List<ShotSession>

    /** The team's reference percentage per zone. */
    suspend fun zoneRefs(): Map<Zone, Int>

    /** Replaces the team's riferimenti, every zone. Staff only: throws [PermissionDeniedException]. */
    suspend fun setZoneRefs(refs: Map<Zone, Int>)

    /** Saves [session] under its [ShotSession.memberId], returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun add(session: ShotSession): ShotSession

    /** Throws [PermissionDeniedException]. */
    suspend fun delete(session: ShotSession)
}
