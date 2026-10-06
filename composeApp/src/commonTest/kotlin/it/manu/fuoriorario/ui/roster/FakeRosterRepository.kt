package it.manu.fuoriorario.ui.roster

import it.manu.fuoriorario.data.EmailTakenException
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.domain.Member

class FakeRosterRepository(vararg members: Member) : RosterRepository {
    val members = members.toMutableList()

    /** Next [add] fails like a network error. */
    var failNextAdd = false

    override suspend fun members() = members.toList()

    override suspend fun add(member: Member) {
        if (failNextAdd) {
            failNextAdd = false
            error("offline")
        }
        if (members.any { it.email == member.email }) throw EmailTakenException()
        members += member
    }
}
