package it.manu.fuoriorario.feature.league

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import it.manu.fuoriorario.domain.Fixture
import it.manu.fuoriorario.domain.ImportFixture
import it.manu.fuoriorario.domain.ImportPreview
import it.manu.fuoriorario.domain.League
import it.manu.fuoriorario.domain.LeagueTeam
import it.manu.fuoriorario.domain.Leg
import it.manu.fuoriorario.domain.Member
import it.manu.fuoriorario.domain.Role
import it.manu.fuoriorario.feature.auth.FakeAuthRepository
import it.manu.fuoriorario.feature.roster.FakeRosterRepository
import it.manu.fuoriorario.testing.TestApp
import it.manu.fuoriorario.testing.awaitNode
import it.manu.fuoriorario.testing.awaitText
import it.manu.fuoriorario.testing.runAppTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.LocalDateTime

private const val ACK = "2026-10-01T10:00:00Z"

/** #100: the league's fixtures for the whole team; staff import the FIP PDF. */
@OptIn(ExperimentalTestApi::class)
class CalendarTest {
    private val coach = Member("Coach", Role.STAFF, ACK, id = "coach")
    private val luca = Member("Luca B.", Role.PLAYER, ACK, id = "luca", jerseyNumber = "7")

    @Test
    fun playerSeeOurFixturesThenTheWholeLeague() = runAppTest {
        val now = Clock.System.now()
        val league = League(
            "l",
            "U19",
            listOf(LeagueTeam("a", "Artemis", ours = true), LeagueTeam("b", "Vasto"), LeagueTeam("c", "Atessa")),
            listOf(
                Fixture("1", 1, Leg.FIRST, now + 1.days, "c", "a", "Palestra Scuola Media"),
                Fixture("2", 2, Leg.FIRST, now + 8.days, "b", "c")
            )
        )
        setContent {
            TestApp(FakeAuthRepository(luca), FakeRosterRepository(coach, luca), leagues = FakeLeagueRepository(league))
        }

        awaitText("Partite")
        onNodeWithText("Partite").performClick()
        awaitText("1ª ANDATA")
        awaitText("Palestra Scuola Media")
        awaitNode(hasText("Riposo"))
        awaitNode(hasText("Vasto"), count = 0)

        onNodeWithTag("calendar_all").performClick()
        awaitText("Vasto")
        awaitText("Riposa: Artemis")
    }

    @Test
    fun staffImportThePdfMarkOursAndConfirm() = runAppTest {
        val leagues = FakeLeagueRepository().apply {
            preview = ImportPreview(
                "U19 - PRIMA FASE",
                listOf("Artemis", "Atessa"),
                listOf(ImportFixture("000691", 1, Leg.FIRST, LocalDateTime(2026, 11, 9, 18, 30), "Atessa", "Artemis"))
            )
        }
        setContent {
            CompositionLocalProvider(LocalPdfPicker provides { onPicked -> { onPicked { byteArrayOf(1) } } }) {
                TestApp(FakeAuthRepository(coach), FakeRosterRepository(coach, luca), leagues = leagues)
            }
        }

        awaitText("Squadra")
        onNodeWithText("Squadra").performClick()
        onNodeWithTag("team_settings").performClick()
        onNodeWithTag("league_import").performScrollTo().performClick()
        awaitText("U19 - PRIMA FASE")
        onNodeWithTag("ours_Artemis").performScrollTo().performClick()
        onNodeWithTag("league_confirm").performScrollTo().performClick()
        awaitText("Calendario importato")
        assertEquals("Artemis", leagues.league?.ours?.name)

        onNodeWithText("Partite").performClick()
        awaitText("Atessa")
    }
}
