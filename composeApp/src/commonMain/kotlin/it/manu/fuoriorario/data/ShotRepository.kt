package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.ShotSession

/** The signed-in player's shot sessions: RLS limits every call to their own. */
interface ShotRepository {
    /** Newest first. */
    suspend fun sessions(): List<ShotSession>

    /** Saves [session], returning it with its id. Throws [PermissionDeniedException]. */
    suspend fun add(session: ShotSession): ShotSession

    /** Throws [PermissionDeniedException]. */
    suspend fun delete(session: ShotSession)
}
