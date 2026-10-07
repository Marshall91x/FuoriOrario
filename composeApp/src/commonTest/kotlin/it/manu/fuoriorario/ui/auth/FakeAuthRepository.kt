package it.manu.fuoriorario.ui.auth

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.runComposeUiTest
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.allStringArrayResources
import fuoriorario.composeapp.generated.resources.allStringResources
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
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray

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

/** Waits for exactly [count] nodes matching [matcher]: 0 waits for them to go. */
@OptIn(ExperimentalTestApi::class)
suspend fun ComposeUiTest.awaitNode(matcher: SemanticsMatcher, count: Int = 1) {
    var found = 0
    repeat(200) {
        found = onAllNodes(matcher).fetchSemanticsNodes().size
        if (found == count) return
        withContext(Dispatchers.Default) { delay(25) }
    }
    fail("${matcher.description}: $found nodes, expected $count")
}

/**
 * runComposeUiTest with every string already loaded. On web each string is read asynchronously the first time it shows:
 * until then stringResource gives "", so an assert or click right after a screen appears fails depending on which
 * tests ran before. Once loaded they stay in memory and appear from the first frame, as on iOS and Android.
 */
@OptIn(ExperimentalTestApi::class)
fun runAppTest(block: suspend ComposeUiTest.() -> Unit) = runComposeUiTest {
    withContext(Dispatchers.Default) {
        Res.allStringResources.values.forEach { getString(it) }
        Res.allStringArrayResources.values.forEach { getStringArray(it) }
    }
    block()
}
