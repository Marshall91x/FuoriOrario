package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Member

/** The email is already in the team. */
class EmailTakenException : Exception()

/** RLS refused the write: shouldn't happen if the UI respects roles (ARCHITECTURE "Gestione errori"). */
class PermissionDeniedException : Exception()

/** The change would leave the team without staff. */
class LastStaffException : Exception()

/** Staff side of `members`: RLS limits every call to the staff's own team. */
interface RosterRepository {
    /** Everyone in the team, staff included. */
    suspend fun members(): List<Member>

    /** Adds [member] to the caller's team, returning it saved (with its id). Throws [EmailTakenException], [PermissionDeniedException]. */
    suspend fun add(member: Member): Member

    /** Saves name, number, position and role of [member]. Throws [LastStaffException], [PermissionDeniedException]. */
    suspend fun update(member: Member)

    /** Removes [member] and, by cascade, all their data and their account. Throws [LastStaffException], [PermissionDeniedException]. */
    suspend fun remove(member: Member)
}
