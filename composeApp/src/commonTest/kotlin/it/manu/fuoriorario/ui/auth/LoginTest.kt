package it.manu.fuoriorario.ui.auth

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import it.manu.fuoriorario.App
import it.manu.fuoriorario.data.AuthRepository
import it.manu.fuoriorario.data.InvalidCodeException
import it.manu.fuoriorario.data.NotInTeamException
import it.manu.fuoriorario.data.Session
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

private class FakeAuthRepository : AuthRepository {
    private val roster = mapOf("giocatore@example.com" to Member("Luca B.", Role.PLAYER))
    override val session = MutableStateFlow<Session>(Session.SignedOut)

    override suspend fun sendCode(email: String) {
        if (email !in roster) throw NotInTeamException()
    }

    override suspend fun verifyCode(email: String, code: String) {
        if (code != "123456") throw InvalidCodeException()
        session.value = Session.SignedIn(roster.getValue(email))
    }

    override suspend fun signOut() {
        session.value = Session.SignedOut
    }
}

/** Like waitUntilExactlyOneExists, but suspends: on web, resources load asynchronously and a blocking wait starves them. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitText(text: String) {
    repeat(200) {
        if (onAllNodes(hasText(text)).fetchSemanticsNodes().size == 1) return
        withContext(Dispatchers.Default) { delay(25) }
    }
    fail("\"$text\" not shown")
}

@OptIn(ExperimentalTestApi::class)
class LoginTest {
    @Test
    fun otpLogin() = runComposeUiTest {
        setContent { App(FakeAuthRepository()) }

        awaitText("Invia codice")
        onNodeWithTag("email").performTextInput("sconosciuto@example.com")
        onNodeWithText("Invia codice").performClick()
        awaitText("Non sei in nessuna squadra, contatta lo staff.")

        onNodeWithTag("email").performTextReplacement(" Giocatore@Example.com ")
        onNodeWithText("Invia codice").performClick()
        awaitText("Accedi")

        onNodeWithTag("code").performTextInput("000000")
        onNodeWithText("Accedi").performClick()
        awaitText("Codice non valido o scaduto.")

        onNodeWithTag("code").performTextReplacement("123456")
        onNodeWithText("Accedi").performClick()
        awaitText("Luca B.")
        awaitText("Esci")

        onNodeWithText("Esci").performClick()
        awaitText("Invia codice")
    }
}
