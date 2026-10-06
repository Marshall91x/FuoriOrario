package it.manu.fuoriorario.ui.roster

import it.manu.fuoriorario.data.EmailTakenException
import it.manu.fuoriorario.data.LastStaffException
import it.manu.fuoriorario.data.PermissionDeniedException
import it.manu.fuoriorario.data.RosterRepository
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role

class FakeRosterRepository(vararg members: Member) : RosterRepository {
    val members = members.toMutableList()

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun members() = members.toList()

    override suspend fun add(member: Member): Member {
        failIfAsked()
        if (members.any { it.email == member.email }) throw EmailTakenException()
        return member.copy(id = member.email).also { members += it }
    }

    override suspend fun update(member: Member) {
        failIfAsked()
        val i = members.indexOfFirst { it.id == member.id }
        if (i < 0) throw PermissionDeniedException() // Like RLS: no row back.
        if (member.role != Role.STAFF) checkStaffLeft(member)
        members[i] = member
    }

    override suspend fun remove(member: Member) {
        failIfAsked()
        checkStaffLeft(member)
        members.removeAll { it.id == member.id }
    }

    /** Like the keep_one_staff trigger. */
    private fun checkStaffLeft(leaving: Member) {
        if (members.none { it.role == Role.STAFF && it.id != leaving.id }) throw LastStaffException()
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            error("offline")
        }
    }
}
