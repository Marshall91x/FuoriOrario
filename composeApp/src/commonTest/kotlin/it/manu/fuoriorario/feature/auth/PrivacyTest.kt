package it.manu.fuoriorario.feature.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import it.manu.fuoriorario.ui.shots.FakeShotRepository
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PrivacyTest {
    @Test
    fun notAcknowledged_blocksUntilAccepted() = runAppTest {
        setContent {
            TestApp(FakeAuthRepository(Member("Luca B.", Role.PLAYER)), FakeRosterRepository(), FakeShotRepository())
        }

        awaitText("Ho letto")
        // Compose resources keep a backslash before an apostrophe: strings.xml must not escape it.
        awaitText("Su Supabase, con server nell'Unione Europea. Li vedi tu e lo staff della tua squadra.")
        assertTrue(onAllNodes(hasText("+ Registra sessione")).fetchSemanticsNodes().isEmpty())

        onNodeWithText("Ho letto").performScrollTo().performClick()
        awaitText("+ Registra sessione")
        assertTrue(onAllNodes(hasText("Ho letto")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun acknowledged_skipsNotice() = runAppTest {
        setContent {
            TestApp(
                FakeAuthRepository(Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z")),
                FakeRosterRepository(),
                FakeShotRepository()
            )
        }

        awaitText("+ Registra sessione")
        assertTrue(onAllNodes(hasText("Ho letto")).fetchSemanticsNodes().isEmpty())
    }
}
