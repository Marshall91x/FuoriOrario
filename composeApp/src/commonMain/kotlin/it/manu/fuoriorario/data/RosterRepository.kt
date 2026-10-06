package it.manu.fuoriorario.data

import it.manu.fuoriorario.domain.Member

/** The email is already in the team. */
class EmailTakenException : Exception()

/** Staff side of `members`: RLS limits both calls to the staff's own team. */
interface RosterRepository {
    /** Everyone in the team, staff included. */
    suspend fun members(): List<Member>

    /** Adds [member] to the caller's team. Throws [EmailTakenException]. */
    suspend fun add(member: Member)
}
