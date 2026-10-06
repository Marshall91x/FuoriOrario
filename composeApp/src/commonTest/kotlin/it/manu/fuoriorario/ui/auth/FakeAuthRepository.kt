package it.manu.fuoriorario.ui.auth

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import it.manu.fuoriorario.data.AuthRepository
import it.manu.fuoriorario.data.InvalidCodeException
import it.manu.fuoriorario.data.NotInTeamException
import it.manu.fuoriorario.data.Session
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/** [roster], when given, is where [refresh] reloads the signed-in member from. */
class FakeAuthRepository(signedIn: Member? = null, private val roster: FakeRosterRepository? = null) : AuthRepository {
    private val accounts = mapOf("giocatore@example.com" to Member("Luca B.", Role.PLAYER))
    override val session = MutableStateFlow(signedIn?.let { Session.SignedIn(it) } ?: Session.SignedOut)

    override suspend fun sendCode(email: String) {
        if (email !in accounts) throw NotInTeamException()
    }

    override suspend fun verifyCode(email: String, code: String) {
        if (code != "123456") throw InvalidCodeException()
        session.value = Session.SignedIn(accounts.getValue(email))
    }

    override suspend fun acknowledgePrivacy() {
        val s = session.value as Session.SignedIn
        session.value = Session.SignedIn(s.member.copy(privacyAckAt = "2026-10-05T18:00:00Z"))
    }

    override fun refresh() {
        val me = (session.value as? Session.SignedIn)?.member ?: return
        session.value = roster?.members?.find { it.id == me.id }?.let { Session.SignedIn(it) } ?: Session.SignedOut
    }

    override suspend fun signOut() {
        session.value = Session.SignedOut
    }
}

/** Like waitUntilExactlyOneExists, but suspends: on web, resources load asynchronously and a blocking wait starves them. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitText(text: String) = awaitNode(hasText(text))

@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher) {
    repeat(200) {
        if (onAllNodes(matcher).fetchSemanticsNodes().size == 1) return
        withContext(Dispatchers.Default) { delay(25) }
    }
    fail("${matcher.description} not shown")
}
