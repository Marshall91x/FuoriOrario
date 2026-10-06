package it.manu.fuoriorario.ui.roster

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
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AddPlayerTest {
    @Test
    fun staffAddsPlayer() = runComposeUiTest {
        val staff = Member("Coach", Role.STAFF, "2026-10-01T10:00:00Z", email = "staff@example.com")
        val roster = FakeRosterRepository(staff)
        setContent { App(FakeAuthRepository(staff), roster) }

        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        awaitText("INSERISCI LA ROSA")

        onNodeWithTag("player_email").performTextInput("marco")
        onNodeWithTag("player_name").performTextInput("Marco R.")
        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Email non valida.")

        onNodeWithTag("player_email").performTextReplacement("staff@example.com")
        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Questa email è già nella squadra.")

        onNodeWithTag("player_email").performTextReplacement(" Marco@Example.com ")
        onNodeWithTag("player_number").performTextInput("12")
        onNodeWithTag("player_position").performScrollTo().performClick()
        awaitText("Guardia")
        onNodeWithText("Guardia").performClick()

        roster.failNextAdd = true
        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithTag("player_name").assertTextEquals("Marco R.")
        onNodeWithTag("player_number").assertTextEquals("12")

        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Marco R. aggiunto alla rosa")
        awaitText("Guardia")
        onNodeWithTag("player_name").assertTextEquals("")
        kotlin.test.assertEquals("marco@example.com", roster.members.last().email)
    }
}
