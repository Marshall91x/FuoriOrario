package it.manu.fuoriorario.ui.shots

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import it.manu.fuoriorario.ui.roster.FakeRosterRepository
import kotlin.test.Test

/** The shot map: zones named and classed against the team's riferimenti, a tap toggles the detail. */
@OptIn(ExperimentalTestApi::class)
class ShotMapTest {
    private val luca = Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z")

    private fun heat(label: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label)

    @Test
    fun tapShowsAndHidesTheZoneDetail() = runAppTest {
        val shots = FakeShotRepository(
            ShotSession(today(), mapOf(Zone.PIT to Shots(4, 10), Zone.MLS to Shots(5, 10), Zone.TL to Shots(8, 10)))
        )
        // The team's own riferimenti, not the defaults: 40% on 50% is under 85%.
        shots.refs += Zone.PIT to 50
        setContent { TestApp(FakeAuthRepository(luca), FakeRosterRepository(), shots) }
        awaitText("Mappa di tiro")

        onNodeWithContentDescription("Pitturato").assert(heat("Sotto il riferimento"))
        onNodeWithContentDescription("Media sinistra").assert(heat("Sopra il riferimento"))
        onNodeWithContentDescription("Tripla centrale").assert(heat("Nessun tiro"))
        onNodeWithContentDescription("Tiri liberi").assert(heat("Sopra il riferimento"))

        onNodeWithContentDescription("Pitturato").performScrollTo().performClick()
        awaitText("Pitturato: 4 su 10 · 40% · riferimento 50%")
        onNodeWithContentDescription("Pitturato").performClick()
        onAllNodes(hasText("Pitturato: 4 su 10 · 40% · riferimento 50%")).assertCountEquals(0)

        onNodeWithContentDescription("Tripla centrale").performScrollTo().performClick()
        awaitText("Tripla centrale: nessun tiro nel periodo · riferimento 33%")
        onNodeWithContentDescription("Tiri liberi").performScrollTo().performClick()
        awaitText("Tiri liberi: 8 su 10 · 80% · riferimento 70%")
    }
}
