package it.manu.fuoriorario.ui.shots

import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Zone

class FakeShotRepository(vararg sessions: ShotSession) : ShotRepository {
    val sessions = sessions.toMutableList()

    /** Next write fails like a network error. */
    var failNext = false

    var refs = DEFAULT_ZONE_REFS

    override suspend fun sessions(member: Member) =
        sessions.filter { it.memberId == member.id }.sortedByDescending { it.date }

    override suspend fun zoneRefs() = refs

    override suspend fun setZoneRefs(refs: Map<Zone, Int>) {
        failIfAsked()
        this.refs = refs
    }

    override suspend fun add(session: ShotSession): ShotSession {
        failIfAsked()
        return session.copy(id = "s${sessions.size + 1}").also { sessions += it }
    }

    override suspend fun delete(session: ShotSession) {
        failIfAsked()
        sessions.removeAll { it.id == session.id }
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            error("offline")
        }
    }
}
