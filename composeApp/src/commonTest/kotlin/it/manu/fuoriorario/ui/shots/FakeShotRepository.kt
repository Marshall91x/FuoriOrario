package it.manu.fuoriorario.ui.shots

import it.manu.fuoriorario.data.ShotRepository
import it.manu.fuoriorario.domain.ShotSession

class FakeShotRepository(vararg sessions: ShotSession) : ShotRepository {
    val sessions = sessions.toMutableList()

    /** Next write fails like a network error. */
    var failNext = false

    override suspend fun sessions() = sessions.sortedByDescending { it.date }

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
