package it.manu.fuoriorario.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.core.today
import it.manu.fuoriorario.domain.DEFAULT_ZONE_REFS
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.domain.ShotSession
import it.manu.fuoriorario.domain.Shots
import it.manu.fuoriorario.domain.Zone
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.plan.FakePlanRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.feature.shots.FakeShotRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** #19: staff change the riferimenti from Squadra → Impostazioni, and the players' maps follow. */
@OptIn(ExperimentalTestApi::class)
class ZoneRefsSettingsTest {
    private val coach = Member("Coach", Role.STAFF, "2026-10-01T10:00:00Z", id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, "2026-10-01T10:00:00Z", id = "luca")

    private fun heat(label: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, label)

    @Test
    fun staffChangeAndRestoreRefs() = runAppTest {
        // 40% in the paint: under the default 55%, over 30%.
        val shots = FakeShotRepository(ShotSession(today(), mapOf(Zone.PIT to Shots(4, 10)), memberId = "luca"))
        setContent {
            TestApp(
                FakeAuthRepository(coach),
                FakeRosterRepository(coach, luca),
                shots,
                MapSettings(),
                FakePlanRepository()
            )
        }
        awaitText("Mappa di tiro")
        onNodeWithContentDescription("Pitturato").assert(heat("Sotto il riferimento"))

        onNodeWithText("Squadra").performClick()
        onNodeWithTag("team_settings").performClick()
        awaitText("Salva riferimenti")
        awaitText("Default 55%")
        onNodeWithTag("ref_pit").performScrollTo().assertTextEquals("55")

        onNodeWithTag("ref_pit").performTextReplacement("120")
        onNodeWithText("Salva riferimenti").performScrollTo().performClick()
        awaitText("Ogni riferimento è un numero intero da 1 a 100.")
        assertEquals(DEFAULT_ZONE_REFS, shots.refs)

        onNodeWithTag("ref_pit").performScrollTo().performTextReplacement("30")
        onNodeWithText("Salva riferimenti").performScrollTo().performClick()
        awaitText("Riferimenti salvati")
        assertEquals(DEFAULT_ZONE_REFS + (Zone.PIT to 30), shots.refs)

        onNodeWithText("Diario di tiro").performClick()
        awaitText("Mappa di tiro")
        onNodeWithContentDescription("Pitturato").assert(heat("Sopra il riferimento"))

        // Restore fills in the defaults; saving stores them.
        onNodeWithText("Squadra").performClick()
        onNodeWithTag("team_settings").performClick()
        awaitText("Ripristina default")
        onNodeWithText("Ripristina default").performScrollTo().performClick()
        onNodeWithTag("ref_pit").performScrollTo().assertTextEquals("55")
        assertEquals(DEFAULT_ZONE_REFS + (Zone.PIT to 30), shots.refs)
        onNodeWithText("Salva riferimenti").performScrollTo().performClick()
        awaitText("Riferimenti salvati")
        assertEquals(DEFAULT_ZONE_REFS, shots.refs)
    }
}
