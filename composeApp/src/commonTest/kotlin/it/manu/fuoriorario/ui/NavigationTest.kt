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
import kotlin.test.Test
import kotlin.test.assertTrue

private const val ACK = "2026-10-01T10:00:00Z"

@OptIn(ExperimentalTestApi::class)
class NavigationTest {
    @Test
    fun player_seesShotLogAndPlanOnly() = runComposeUiTest {
        setContent { App(FakeAuthRepository(Member("Luca B.", Role.PLAYER, ACK))) }

        awaitText("LAVORO INDIVIDUALE")
        awaitText("Qui arriveranno la mappa di tiro, le statistiche e le sessioni.")
        onNodeWithText("Piano").performClick()
        awaitText("Qui arriverà il piano settimanale con gli esercizi.")
        assertTrue(onAllNodes(hasText("Squadra")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun staff_reachesTeam() = runComposeUiTest {
        setContent { App(FakeAuthRepository(Member("Coach", Role.STAFF, ACK))) }

        awaitText("VISTA STAFF")
        onNodeWithText("Squadra").performClick()
        awaitText("Qui arriveranno la rosa e le impostazioni della squadra.")
        onNodeWithText("Diario di tiro").performClick()
        awaitText("Qui arriveranno la mappa di tiro, le statistiche e le sessioni.")
    }
}
