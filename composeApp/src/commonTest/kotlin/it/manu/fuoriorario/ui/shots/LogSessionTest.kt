package it.manu.fuoriorario.ui.shots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import it.manu.fuoriorario.App
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** UI test 2 (ARCHITECTURE "Test"): log a session, with validation. */
@OptIn(ExperimentalTestApi::class)
class LogSessionTest {
    private val luca = Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z", jerseyNumber = "7", position = "Guardia")
    private val older = ShotSession(LocalDate(2026, 9, 3), mapOf(Zone.PIT to Shots(3, 5)), "Prima", id = "old")

    @Test
    fun playerLogsSessionWithValidation() = runComposeUiTest {
        val shots = FakeShotRepository(older)
        setContent { App(FakeAuthRepository(luca), FakeRosterRepository(), shots) }
        awaitText("3/5 · 60%")
        awaitText("3 set")
        onNodeWithText("+ Registra sessione").performClick()
        awaitText("Salva sessione")

        onNodeWithText("Salva sessione").performScrollTo().performClick()
        awaitText("Inserisci almeno una zona con dei tentativi.")

        // + on made raises attempted; then attempted goes back under made.
        onNodeWithTag("made_pit_plus").performScrollTo().performClick()
        onNodeWithTag("made_pit_plus").performScrollTo().performClick()
        onNodeWithTag("attempted_pit").assertTextEquals("2")
        onNodeWithTag("attempted_pit_minus").performScrollTo().performClick()
        onNodeWithText("Salva sessione").performScrollTo().performClick()
        awaitText("Pitturato: i canestri segnati non possono superare i tentativi.")

        onNodeWithTag("attempted_pit_plus").performScrollTo().performClick()
        onNodeWithTag("attempted_pit").assertTextEquals("5")
        onNodeWithTag("attempted_tl_plus").performScrollTo().performClick()
        onNodeWithTag("attempted_tl_plus").performScrollTo().performClick()
        onNodeWithTag("made_tl").performScrollTo().performTextReplacement("7")
        onNodeWithTag("session_note").performScrollTo().performTextInput("Gambe stanche")

        // A failed save keeps the sheet and what was typed.
        shots.failNext = true
        onNodeWithText("Salva sessione").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithTag("made_tl").assertTextEquals("7")

        onNodeWithText("Salva sessione").performScrollTo().performClick()
        awaitText("Sessione salvata")
        awaitText("2/5 · 40%")
        awaitText("· TL 7/10")
        awaitText("Gambe stanche")
        assertEquals(
            ShotSession(
                Clock.System.todayIn(TimeZone.currentSystemDefault()),
                mapOf(Zone.PIT to Shots(2, 5), Zone.TL to Shots(7, 10)),
                "Gambe stanche",
                id = "s2"
            ),
            shots.sessions.last()
        )
    }

    @Test
    fun playerDeletesSessionWithConfirmation() = runComposeUiTest {
        val shots = FakeShotRepository(older)
        setContent { App(FakeAuthRepository(luca), FakeRosterRepository(), shots) }
        awaitText("Prima")
        onNodeWithTag("delete_old").performClick()
        awaitText("Conferma")
        assertEquals(1, shots.sessions.size)
        onNodeWithTag("delete_old").performClick()
        awaitText("Sessione eliminata")
        awaitText("Nessuna sessione. Registra la prima dopo il prossimo allenamento.")
        assertEquals(emptyList(), shots.sessions)
    }
}
