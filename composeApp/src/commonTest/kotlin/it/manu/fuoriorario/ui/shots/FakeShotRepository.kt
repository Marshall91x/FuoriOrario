package it.manu.fuoriorario.ui.shots

import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Zone

class FakeShotRepository(vararg sessions: ShotSession) : ShotRepository {
    val sessions = sessions.toMutableList()

    /** Next write fails like a network error. */
    var failNext = false

    /** The seed's defaults. */
    var refs = mapOf(
        Zone.PIT to 55, Zone.MLS to 40, Zone.MLC to 40, Zone.MLD to 40, Zone.ACS to 36,
        Zone.ALS to 33, Zone.CEN to 33, Zone.ALD to 33, Zone.ACD to 36, Zone.TL to 70
    )

    override suspend fun sessions() = sessions.sortedByDescending { it.date }

    override suspend fun zoneRefs() = refs

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
