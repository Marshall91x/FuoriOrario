package it.manu.fuoriorario.ui.shots

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.russhwolf.settings.MapSettings
import it.manu.fuoriorario.App
import it.manu.fuoriorario.core.today
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
import kotlin.test.assertTrue

private const val ACK = "2026-10-01T10:00:00Z"

/** #12: staff pick a player in the header and work on their Diario di tiro. */
@OptIn(ExperimentalTestApi::class)
class StaffShotLogTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, jerseyNumber = "7", id = "luca")
    private val marco = Member("Marco", Role.PLAYER, ACK, jerseyNumber = "12", id = "marco")
    private val anna = Member("Anna", Role.PLAYER, ACK, id = "anna")

    @Test
    fun staffFollowsAnyPlayer() = runComposeUiTest {
        val shots = FakeShotRepository(
            ShotSession(today(), mapOf(Zone.PIT to Shots(3, 5)), id = "l1", memberId = "luca"),
            ShotSession(today(), mapOf(Zone.PIT to Shots(1, 4)), id = "m1", memberId = "marco")
        )
        val roster = FakeRosterRepository(anna, coach, marco, luca)
        val prefs = MapSettings()
        var launch by mutableIntStateOf(0)
        setContent { key(launch) { App(FakeAuthRepository(coach), roster, shots, prefs) } }

        // First in roster order, only that player's sessions.
        awaitText("3/5 · 60%")
        onNodeWithTag("player_picker").assertTextEquals("#7 · Luca B.")
        assertTrue(onAllNodesWithText("1/4 · 25%").fetchSemanticsNodes().isEmpty())

        // The period stays when switching player.
        awaitText("7 giorni")
        onNodeWithTag("period_days_7").performClick()
        onNodeWithTag("player_picker").performClick()
        awaitText("#12 · Marco")
        // Roster order, players only.
        assertEquals(
            listOf("#7 · Luca B.", "#12 · Marco", "Anna"),
            onAllNodesWithTag("player_option").fetchSemanticsNodes().map {
                it.config[SemanticsProperties.Text].joinToString()
            }
        )
        onNodeWithText("#12 · Marco").performClick()
        awaitText("1/4 · 25%")
        awaitText("MARCO")
        onNodeWithTag("period_days_7").assertIsSelected()

        // And the pick survives restarting the app.
        launch++
        awaitText("1/4 · 25%")
        onNodeWithTag("player_picker").assertTextEquals("#12 · Marco")

        // The choice survives a trip to another tab.
        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        awaitText("ROSA")
        onNodeWithText("Diario di tiro").performClick()
        awaitText("1/4 · 25%")
        onNodeWithTag("player_picker").assertTextEquals("#12 · Marco")

        // Logged under Marco.
        awaitText("+ Registra sessione")
        onNodeWithText("+ Registra sessione").performClick()
        awaitText("Salva sessione")
        onNodeWithTag("made_tl").performScrollTo().performTextReplacement("6")
        onNodeWithText("Salva sessione").performScrollTo().performClick()
        awaitText("Sessione salvata")
        assertEquals("marco", shots.sessions.last().memberId)

        // And deleted for him.
        onNodeWithTag("delete_m1").performScrollTo().performClick()
        awaitText("Conferma")
        onNodeWithTag("delete_m1").performScrollTo().performClick()
        awaitText("Sessione eliminata")
        assertTrue(shots.sessions.none { it.id == "m1" })
    }

    @Test
    fun playerHasNoPicker() = runComposeUiTest {
        setContent {
            App(FakeAuthRepository(luca), FakeRosterRepository(coach, luca, marco), FakeShotRepository(), MapSettings())
        }

        awaitText("+ Registra sessione")
        awaitText("Luca B.")
        assertTrue(onAllNodesWithTag("player_picker").fetchSemanticsNodes().isEmpty())
    }
}
