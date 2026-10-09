package it.manu.fuoriorario.feature.shots

import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.shots.data.ShotRepository
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException

class FakeShotRepository(vararg sessions: ShotSession) : ShotRepository {
    val sessions = sessions.toMutableList()

    /** Next write fails like a network error. */
    var failNext = false

    var refs = DEFAULT_ZONE_REFS

    /** Next team load fails like a network error. */
    var failTeam = false

    /** Next player's load fails without network, so the screen offers "Riprova". */
    var failLoad = false

    /** Next riferimenti load fails without network, so the screen offers "Riprova". */
    var failRefs = false

    override suspend fun sessions(member: Member): List<ShotSession> {
        if (failLoad) {
            failLoad = false
            throw IOException("offline")
        }
        return sessions.filter { it.memberId == member.id }.sortedByDescending { it.date }
    }

    override suspend fun teamSessions(since: LocalDate): List<ShotSession> {
        if (failTeam) {
            failTeam = false
            throw IOException("offline")
        }
        return sessions.filter { it.date >= since }
    }

    override suspend fun zoneRefs(): Map<Zone, Int> {
        if (failRefs) {
            failRefs = false
            throw IOException("offline")
        }
        return refs
    }

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
