package it.manu.fuoriorario.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import it.manu.fuoriorario.App
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.auth.FakeAuthRepository
import it.manu.fuoriorario.ui.auth.awaitText
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test
import kotlin.test.assertTrue

private const val ACK = "2026-10-01T10:00:00Z"

@OptIn(ExperimentalTestApi::class)
class NavigationTest {
    @Test
    fun player_seesShotLogAndPlanOnly() = runComposeUiTest {
        setContent {
            App(FakeAuthRepository(Member("Luca B.", Role.PLAYER, ACK)), FakeRosterRepository(), FakeShotRepository())
        }

        awaitText("LAVORO INDIVIDUALE")
        awaitText("+ Registra sessione")
        awaitText("Piano")
        onNodeWithText("Piano").performClick()
        awaitText("Qui arriverà il piano settimanale con gli esercizi.")
        assertTrue(onAllNodes(hasText("Squadra")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun staff_reachesTeam() = runComposeUiTest {
        setContent { App(FakeAuthRepository(Member("Coach", Role.STAFF, ACK)), FakeRosterRepository()) }

        awaitText("VISTA STAFF")
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        awaitText("INSERISCI LA ROSA")
        awaitText("Diario di tiro")
        onNodeWithText("Diario di tiro").performClick()
        // No players yet: nobody to follow.
        awaitText("INSERISCI LA ROSA")
        assertTrue(onAllNodes(hasText("+ Registra sessione")).fetchSemanticsNodes().isEmpty())
    }
}
