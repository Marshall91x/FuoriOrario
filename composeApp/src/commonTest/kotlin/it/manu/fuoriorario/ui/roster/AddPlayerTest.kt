package it.manu.fuoriorario.ui.roster

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import it.manu.fuoriorario.App
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitNode
import it.manu.fuoriorario.ui.auth.awaitText
import it.manu.fuoriorario.ui.auth.runAppTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AddPlayerTest {
    @Test
    fun staffAddsPlayer() = runAppTest {
        val staff = Member("Coach", Role.STAFF, "2026-10-01T10:00:00Z", email = "staff@example.com")
        val roster = FakeRosterRepository(staff)
        setContent { App(FakeAuthRepository(staff), roster) }

        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        onNodeWithTag("team_roster").performClick()
        // Not by its title: the Diario without players shows it too.
        awaitNode(hasTestTag("add_email"))

        onNodeWithTag("add_email").performTextInput("marco")
        onNodeWithTag("add_name").performTextInput("Marco R.")
        awaitText("Aggiungi")
        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Email non valida.")

        onNodeWithTag("add_email").performTextReplacement("staff@example.com")
        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Questa email è già nella squadra.")

        onNodeWithTag("add_email").performTextReplacement(" Marco@Example.com ")
        onNodeWithTag("add_number").performTextInput("12")
        onNodeWithTag("add_position").performScrollTo().performClick()
        awaitText("Guardia")
        onNodeWithText("Guardia").performClick()

        roster.failNext = true
        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Salvataggio non riuscito. Riprova tra poco.")
        onNodeWithTag("add_name").assertTextEquals("Marco R.")
        onNodeWithTag("add_number").assertTextEquals("12")

        onNodeWithText("Aggiungi").performScrollTo().performClick()
        awaitText("Marco R. aggiunto alla rosa")
        awaitText("Guardia")
        onNodeWithTag("add_name").assertTextEquals("")
        assertEquals("marco@example.com", roster.members.last().email)
    }
}
