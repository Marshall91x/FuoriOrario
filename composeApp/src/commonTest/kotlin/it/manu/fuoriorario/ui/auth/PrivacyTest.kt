package it.manu.fuoriorario.ui.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import it.manu.fuoriorario.App
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PrivacyTest {
    @Test
    fun notAcknowledged_blocksUntilAccepted() = runComposeUiTest {
        setContent {
            App(FakeAuthRepository(Member("Luca B.", Role.PLAYER)), FakeRosterRepository(), FakeShotRepository())
        }

        awaitText("Ho letto")
        assertTrue(onAllNodes(hasText("+ Registra sessione")).fetchSemanticsNodes().isEmpty())

        onNodeWithText("Ho letto").performScrollTo().performClick()
        awaitText("+ Registra sessione")
        assertTrue(onAllNodes(hasText("Ho letto")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun acknowledged_skipsNotice() = runComposeUiTest {
        setContent {
            App(
                FakeAuthRepository(Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z")),
                FakeRosterRepository(),
                FakeShotRepository()
            )
        }

        awaitText("+ Registra sessione")
        assertTrue(onAllNodes(hasText("Ho letto")).fetchSemanticsNodes().isEmpty())
    }
}
