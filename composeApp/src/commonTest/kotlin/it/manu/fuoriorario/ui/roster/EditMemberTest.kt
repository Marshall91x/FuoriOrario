package it.manu.fuoriorario.ui.roster

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import it.manu.fuoriorario.App
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EditMemberTest {
    private val staff = Member("Coach", Role.STAFF, "2026-10-01T10:00:00Z", email = "staff@example.com", id = "s")
    private val vice = Member("Vice", Role.STAFF, email = "vice@example.com", id = "v")
    private val luca = Member("Luca B.", Role.PLAYER, email = "luca@example.com", jerseyNumber = "7", id = "l")

    @Test
    fun staffEditsMember() = runComposeUiTest {
        val roster = FakeRosterRepository(staff, luca)
        setContent { App(FakeAuthRepository(staff), roster) }
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()

        awaitText("Luca B.")
        onNodeWithText("Luca B.").performClick()
        awaitText("Salva modifiche")
        onNodeWithTag("edit_name").performTextReplacement("")
        onNodeWithText("Salva modifiche").performScrollTo().performClick()
        awaitText("Il nome deve avere da 1 a 40 caratteri.")

        onNodeWithTag("edit_name").performTextReplacement("Luca Bianchi")
        onNodeWithTag("edit_number").performTextReplacement("8")
        onNodeWithTag("edit_role_staff").performScrollTo().performClick()
        onNodeWithText("Salva modifiche").performScrollTo().performClick()
        awaitText("Modifiche salvate")
        awaitText("Luca Bianchi")
        assertEquals(
            luca.copy(displayName = "Luca Bianchi", jerseyNumber = "8", role = Role.STAFF),
            roster.members.last()
        )
    }

    @Test
    fun staffRemovesMemberWithConfirmation() = runComposeUiTest {
        val roster = FakeRosterRepository(staff, luca)
        setContent { App(FakeAuthRepository(staff), roster) }
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()

        awaitText("Luca B.")
        onNodeWithTag("remove_l").performClick()
        awaitText("Conferma")
        assertEquals(2, roster.members.size)
        onNodeWithTag("remove_l").performClick()
        awaitText("Luca B. tolto dalla rosa")
        assertEquals(listOf(staff), roster.members)

        // The last staff can't remove themselves.
        onNodeWithTag("remove_s").performScrollTo().performClick()
        onNodeWithTag("remove_s").performClick()
        awaitText("Serve almeno un membro dello staff nella squadra.")
        assertEquals(listOf(staff), roster.members)
    }

    @Test
    fun staffDemotingThemselvesBecomesPlayer() = runComposeUiTest {
        val roster = FakeRosterRepository(staff, vice)
        setContent { App(FakeAuthRepository(staff, roster), roster) }
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()

        awaitText("Vice")
        onAllNodes(hasText("Coach"))[1].performClick() // [0] is the header
        awaitText("Salva modifiche")
        onNodeWithTag("edit_role_player").performScrollTo().performClick()
        onNodeWithText("Salva modifiche").performScrollTo().performClick()

        awaitText("LAVORO INDIVIDUALE")
        awaitText("Qui arriveranno la mappa di tiro, le statistiche e le sessioni.")
        assertTrue(onAllNodes(hasText("Squadra")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun staffRemovingThemselvesIsSignedOut() = runComposeUiTest {
        val roster = FakeRosterRepository(staff, vice)
        setContent { App(FakeAuthRepository(staff, roster), roster) }
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()

        awaitText("Vice")
        onNodeWithTag("remove_s").performClick()
        onNodeWithTag("remove_s").performClick()
        awaitText("Invia codice")
        assertEquals(listOf(vice), roster.members)
    }
}
