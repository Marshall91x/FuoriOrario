package it.manu.fuoriorario.feature.auth

import it.manu.fuoriorario.core.session.Session
import it.manu.fuoriorario.core.session.SessionExpiry
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.feature.auth.data.AuthRepository
import it.manu.fuoriorario.feature.auth.data.InvalidCodeException
import it.manu.fuoriorario.feature.auth.data.NotInTeamException
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** [roster], when given, is where [refresh] reloads the signed-in member from. */
class FakeAuthRepository(signedIn: Member? = null, private val roster: FakeRosterRepository? = null) :
    AuthRepository,
    SessionExpiry {
    private val accounts = mapOf("giocatore@example.com" to Member("Luca B.", Role.PLAYER))
    override val session = MutableStateFlow<Session>(signedIn?.let { Session.SignedIn(it) } ?: Session.SignedOut)

    /** Next call fails like a network error. */
    var failNext = false

    /** What [sendCode] was last called with. */
    var sentTo: String? = null

    var refreshes = 0

    override suspend fun sendCode(email: String) {
        failIfAsked()
        sentTo = email
        if (email !in accounts) throw NotInTeamException()
    }

    override suspend fun verifyCode(email: String, code: String) {
        failIfAsked()
        if (code != "123456") throw InvalidCodeException()
        session.value = Session.SignedIn(accounts.getValue(email))
    }

    override suspend fun acknowledgePrivacy() {
        failIfAsked()
        val s = session.value as Session.SignedIn
        session.value = Session.SignedIn(s.member.copy(privacyAckAt = "2026-10-05T18:00:00Z"))
    }

    override fun refresh() {
        refreshes++
        val me = (session.value as? Session.SignedIn)?.member ?: return
        session.value = roster?.members?.find { it.id == me.id }?.let { Session.SignedIn(it) } ?: Session.SignedOut
    }

    override suspend fun signOut() {
        session.value = Session.SignedOut
    }

    override fun expire() {
        session.value = Session.Expired
    }

    private fun failIfAsked() {
        if (failNext) {
            failNext = false
            error("offline")
        }
    }
}
